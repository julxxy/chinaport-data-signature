package cn.alphahub.eport.signature.core;

import cn.alphahub.dtt.plus.util.JacksonUtil;
import cn.alphahub.eport.signature.config.UkeyProperties;
import cn.alphahub.eport.signature.core.notify.EmailNotifyStrategy;
import cn.alphahub.eport.signature.core.notify.model.EmailNotifyRecord;
import cn.alphahub.eport.signature.entity.UkeyResponse;
import cn.alphahub.eport.signature.entity.WebSocketWrapper;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONUtil;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import static cn.alphahub.eport.signature.base.constant.FrameworkConstant.TRACE_ID;

/**
 * 加签websocket客户端基类
 * <ul><li>全参数构造函数注入IOC</li></ul>
 *
 * @author weasley
 * @version 1.2.0
 * @date 2022/2/15
 */
@Slf4j
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class WebSocketClientHandler extends TextWebSocketHandler {
    /**
     * 电子口岸u-key的配置参数
     */
    private final UkeyProperties ukeyProperties;
    /**
     * WebSocket包装类
     */
    private final WebSocketWrapper webSocketWrapper;
    /**
     * X509Certificate证书判断
     */
    private final CertificateHandler certificateHandler;

    /**
     * 电子口岸u-key加签失败通知策略
     */
    @Autowired
    @Qualifier(EmailNotifyStrategy.NAME)
    private EmailNotifyStrategy emailNotifyStrategy;

    public WebSocketClientHandler(UkeyProperties ukeyProperties, WebSocketWrapper webSocketWrapper, CertificateHandler certificateHandler) {
        this.ukeyProperties = ukeyProperties;
        this.webSocketWrapper = webSocketWrapper;
        this.certificateHandler = certificateHandler;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        MDC.put(TRACE_ID, webSocketWrapper.getSessionId());
        log.warn("已和 [{}] 建立 websocket 连接...", ukeyProperties.getWsUrl());
        session.sendMessage(new TextMessage(webSocketWrapper.getPayload()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        MDC.put(TRACE_ID, webSocketWrapper.getSessionId());
        log.warn("收到ukey响应数据: {}", message.getPayload());
        UkeyResponse ukeyResponse = JSONUtil.toBean(message.getPayload(), new TypeReference<>() {
        }, true);
        if (Objects.equals(ukeyResponse.get_id(), webSocketWrapper.getRequest().getId())) {
            try {
                UkeyResponse.Args responseArgs = ukeyResponse.get_args();
                if (responseArgs.getResult().equals(true) && CollectionUtils.isNotEmpty(responseArgs.getData())) {
                    log.warn("电子口岸u-key加签数据成功：{}", JacksonUtil.toJson(responseArgs));
                    webSocketWrapper.getSignResult().setSuccess(true);
                    webSocketWrapper.getSignResult().setSignatureValue(responseArgs.getData().get(0));
                    webSocketWrapper.getSignResult().setCertNo(responseArgs.getData().get(1));
                    if (SignHandler.isSignXml(webSocketWrapper.getRequest())) {
                        webSocketWrapper.getSignResult().setX509Certificate(certificateHandler.getX509Certificate(ukeyResponse.get_method()));
                    }
                } else {
                    log.error("电子口岸u-key加签数据失败：{}", JacksonUtil.toJson(responseArgs));
                    this.sendNotification(new EmailNotifyRecord(webSocketWrapper, ukeyResponse));
                }
            } catch (Exception e) {
                webSocketWrapper.getSignResult().setSuccess(false);
                log.error("唤醒线程异常 {}", e.getLocalizedMessage(), e);
            } finally {
                LockSupport.unpark(webSocketWrapper.getThreadReference().get());
            }
        }
    }

    /**
     * 发送电子口岸u-key加签失败通知
     *
     * @param record 电子口岸u-key加签失败通知记录
     * @throws Exception ex
     */
    private void sendNotification(EmailNotifyRecord record) throws Exception {
        this.emailNotifyStrategy.notify(record);
    }
}
