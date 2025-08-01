package cn.alphahub.eport.signature.core;

import cn.alphahub.eport.signature.base.constant.FrameworkConstant;
import cn.alphahub.eport.signature.config.UkeyInitialConfig;
import cn.alphahub.eport.signature.config.UkeyProperties;
import cn.alphahub.eport.signature.core.notify.EmailNotifyStrategy;
import cn.alphahub.eport.signature.core.notify.model.EmailNotifyRecord;
import cn.alphahub.eport.signature.entity.SignRequest;
import cn.alphahub.eport.signature.entity.SignResult;
import cn.alphahub.eport.signature.entity.UkeyRequest;
import cn.alphahub.eport.signature.entity.UkeyResponse;
import cn.alphahub.eport.signature.entity.UkeyResponse.Args;
import cn.alphahub.eport.signature.entity.UkeyResponseArgsWrapper;
import cn.alphahub.eport.signature.entity.WebSocketWrapper;
import cn.alphahub.eport.signature.support.XMLValidator;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import static cn.alphahub.dtt.plus.util.JacksonUtil.toPrettyJson;


/**
 * <p>电子口岸加签业务核心类</p>
 * <a href='http://tool.qdhuaxun.cn/?signdoc'>华讯云业务帮助文档，签名的不要看误导人，华讯的业务文档可以参考</a>
 *
 * @author weasley
 * @version 1.0
 * @date 2022/2/12
 */
@Data
@Slf4j
@Service
@Validated
@Accessors(chain = true)
public class SignHandler {
    @Autowired
    private UkeyProperties ukeyProperties;
    @Autowired
    private WebSocketClientHandler webSocketClientHandler;
    @Autowired
    private StandardWebSocketClient standardWebSocketClient;
    /**
     * 电子口岸u-key加签失败通知策略
     */
    @Autowired
    @Qualifier(EmailNotifyStrategy.NAME)
    private EmailNotifyStrategy emailNotifyStrategy;

    /**
     * 获取ukey的socket入参的inData字段
     *
     * @param request 加签数据请求入参
     * @return ukey的socket入参的inData字段
     */
    public static String getInitData(@Valid SignRequest request) {
        if (isSignXml(request)) {
            return SignatureHandler.getSignatureNodeBeforeSend(request);
        }
        return request.getData();
    }

    /**
     * 判断是否总署xml
     *
     * @param request 加签数据请求入参
     * @return 总署xml返回true
     */
    public static boolean isSignXml(@Valid SignRequest request) {
        if (StringUtils.isBlank(request.getData())) {
            throw new IllegalArgumentException("加签数据请求入参能为空!");
        }
        boolean isSignXmlString = XMLValidator.isValidXML(request.getData()) || Strings.CS.startsWith(request.getData(), "<ceb:CEB");
        boolean isSign179String = Strings.CS.startsWith(request.getData(), "\"sessionID\"");
        if (isSignXmlString && !isSign179String) {
            return true;
        }
        if (isSign179String && !isSignXmlString) {
            return false;
        }
        throw new IllegalArgumentException("加签数据请求入参不合法,请检查参数:" + request.getData());
    }

    /**
     * 获取u-key签名参数
     *
     * @return 发送u-key的签名的入参
     * @since 2022-11-27
     */
    public String getSignDataParameter(@Valid SignRequest request) {
        return UkeyInitialConfig.getSignDataAsPEM(request);
    }

    /**
     * WebSocket发送消息给电子口岸u-key
     *
     * @param request 加签请求参数
     * @param payload websocket发送的数据载荷
     */
    public SignResult sign(@Valid SignRequest request, @NotBlank(message = "websocket发送的数据载荷不能为空") String payload) {
        log.info("收到u-key加签数据: {}", payload);

        WebSocketWrapper wsWrapper = webSocketClientHandler.getWebSocketWrapper();
        wsWrapper.setPayload(payload);
        wsWrapper.setRequest(request);
        wsWrapper.setSignResult(new SignResult());
        wsWrapper.setThreadReference(new AtomicReference<>(Thread.currentThread()));
        wsWrapper.setSessionId(MDC.get(FrameworkConstant.TRACE_ID));

        if (SignHandler.isSignXml(request)) {
            wsWrapper.getSignResult().setDigestValue(SignatureHandler.getDigestValueOfCEBXxxMessage(request.getData()));
            wsWrapper.getSignResult().setSignatureNode(SignatureHandler.getSignatureNodeBeforeSend(request));
        }

        WebSocketConnectionManager wsConnManager = new WebSocketConnectionManager(standardWebSocketClient, webSocketClientHandler, ukeyProperties.getWsUrl());
        wsConnManager.start();

        try {
            LockSupport.parkNanos(wsWrapper.getThreadReference().get(), 1000 * 1000 * 1000 * 5L);
        } catch (Exception e) {
            log.error("线程自动unpark异常 {}", e.getLocalizedMessage(), e);
            wsWrapper.getSignResult().setSuccess(false);
        } finally {
            wsConnManager.stop();
        }

        return wsWrapper.getSignResult();
    }

    /**
     * 获取u-key加签返回内层对象（我们要的数据在这里面）
     *
     * @apiNote 发送任意数据给ukey，实时响应结果
     */
    public Args getUkeyResponseArgs(UkeyRequest ukeyRequest) {

        AtomicReference<UkeyResponseArgsWrapper> ref = new AtomicReference<>();
        ref.set(new UkeyResponseArgsWrapper(Thread.currentThread(), new Args(), MDC.get(FrameworkConstant.TRACE_ID)));

        WebSocketConnectionManager wsConnManager = new WebSocketConnectionManager(standardWebSocketClient, new TextWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(ukeyRequest)));
            }

            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                MDC.put(FrameworkConstant.TRACE_ID, ref.get().getSessionId());
                UkeyResponse response = JSONUtil.toBean(message.getPayload(), new TypeReference<>() {
                }, true);
                try {
                    if (Objects.equals(response.get_id(), ukeyRequest.get_id())) {
                        log.warn("从电子口岸ukey中获取获取到数据: {} {}", response.get_args(), response);
                        if (Objects.equals(response.get_args().getResult(), false)) {
                            log.error("电子口岸ukey加签遇到错误: {}", response.get_args().getError());
                            this.sendNotify(response);
                        } else {
                            log.info("电子口岸ukey返回成功: {}", response.get_args().getData());
                        }
                        ref.get().setResponseArgs(response.get_args());
                    }
                } catch (Exception e) {
                    log.error("唤醒线程异常 {}", e.getLocalizedMessage(), e);
                }
            }

            /**
             * 发送通知
             *
             * @param response ukey响应结果
             */
            private void sendNotify(UkeyResponse response) {
                WebSocketWrapper wsWrapper = new WebSocketWrapper();
                wsWrapper.setSessionId(ref.get().getSessionId());
                wsWrapper.setPayload(toPrettyJson(ukeyRequest.getArgs()));
                wsWrapper.setThreadReference(new AtomicReference<>(ref.get().getThread()));
                SignRequest signRequest = new SignRequest(ukeyRequest.get_id(), toPrettyJson(ukeyRequest.getArgs()));
                wsWrapper.setRequest(signRequest);
                SignResult signResult = new SignResult().setSuccess(false);
                wsWrapper.setSignResult(signResult);
                emailNotifyStrategy.notify(new EmailNotifyRecord(wsWrapper, response));
            }

        }, ukeyProperties.getWsUrl());

        wsConnManager.start();

        try {
            LockSupport.parkNanos(ref.get().getThread(), 1000 * 1000 * 1000 * 1L);
        } catch (Exception e) {
            log.error("线程自动unpark异常 {}", e.getLocalizedMessage(), e);
        } finally {
            wsConnManager.stop();
        }

        return ref.get().getResponseArgs();
    }

}
