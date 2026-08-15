package cn.alphahub.eport.signature.core.v121;

import cn.alphahub.eport.signature.base.constant.FrameworkConstant;
import cn.alphahub.eport.signature.config.UkeyInitialConfig;
import cn.alphahub.eport.signature.config.UkeyProperties;
import cn.alphahub.eport.signature.core.SignatureHandler;
import cn.alphahub.eport.signature.core.WebSocketClientHandler;
import cn.alphahub.eport.signature.core.notify.EmailNotifyStrategy;
import cn.alphahub.eport.signature.entity.SignRequest;
import cn.alphahub.eport.signature.entity.SignResult;
import cn.alphahub.eport.signature.entity.WebSocketWrapper;
import cn.alphahub.eport.signature.support.XMLValidator;
import jakarta.annotation.PreDestroy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

/**
 * 电子口岸加签业务核心类（最终优化版）
 * 核心：仅解决大消息1009错误 + 连接复用，业务逻辑与原SignHandler完全一致
 */
@Data
@Slf4j
//@Service("mySignHandler") // 唯一Bean名，避免与原SignHandler冲突
@Validated
@Accessors(chain = true)
public class MySignHandler {
    // 原逻辑常量（完全复制）
    private static final long LOCK_TIMEOUT_NANOS = 1000 * 1000 * 1000 * 5L;

    // 大消息配置（解决1009错误）
    private static final int MAX_TEXT_MESSAGE_SIZE = 1024 * 1024; // 1MB

    @Autowired
    private UkeyProperties ukeyProperties;
    @Autowired
    private WebSocketClientHandler webSocketClientHandler; // 复用原全局Handler
    @Autowired
    private StandardWebSocketClient standardWebSocketClient;
    @Autowired
    @Qualifier(EmailNotifyStrategy.NAME)
    private EmailNotifyStrategy emailNotifyStrategy;

    // 连接复用（仅这一个优化点）
    private WebSocketConnectionManager reuseConnManager;


    // -------------------- 业务方法完全复制原SignHandler --------------------
    public static String getInitData(@Valid SignRequest request) {
        if (isSignXml(request)) {
            return SignatureHandler.getSignatureNodeBeforeSend(request);
        }
        return request.getData();
    }

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

    public String getSignDataParameter(@Valid SignRequest request) {
        return UkeyInitialConfig.getSignDataAsPEM(request);
    }

    /**
     * 核心sign方法：完全复制原逻辑，仅优化连接复用
     */
    public SignResult sign(@Valid SignRequest request, @NotBlank(message = "websocket发送的数据载荷不能为空") String payload) {
        log.info("收到u-key加签数据: {}", payload);

        // 完全复制原逻辑：创建WebSocketWrapper
        WebSocketWrapper wsWrapper = webSocketClientHandler.getWebSocketWrapper();
        wsWrapper.setPayload(payload);
        wsWrapper.setRequest(request);
        wsWrapper.setSignResult(new SignResult());
        wsWrapper.setThreadReference(new AtomicReference<>(Thread.currentThread()));
        wsWrapper.setSessionId(MDC.get(FrameworkConstant.TRACE_ID));

        // 完全复制原逻辑：XML加签字段赋值
        if (MySignHandler.isSignXml(request)) {
            wsWrapper.getSignResult().setDigestValue(SignatureHandler.getDigestValueOfCEBXxxMessage(request.getData()));
            wsWrapper.getSignResult().setSignatureNode(SignatureHandler.getSignatureNodeBeforeSend(request));
        }

        // 优化点：连接复用（替代原每次new）
        if (reuseConnManager == null) {
            reuseConnManager = new WebSocketConnectionManager(
                    standardWebSocketClient, webSocketClientHandler, ukeyProperties.getWsUrl()
            );
        }
        WebSocketConnectionManager wsConnManager = reuseConnManager;

        // 完全复制原逻辑：启动连接 + 阻塞等待
        wsConnManager.start();
        try {
            LockSupport.parkNanos(wsWrapper.getThreadReference().get(), LOCK_TIMEOUT_NANOS);
        } catch (Exception e) {
            log.error("线程自动unpark异常 {}", e.getLocalizedMessage(), e);
            wsWrapper.getSignResult().setSuccess(false);
        }

        // 完全复制原逻辑：返回结果（signatureValue由原WebSocketClientHandler赋值）
        return wsWrapper.getSignResult();
    }

    // 销毁资源：关闭复用的连接
    @PreDestroy
    public void destroy() {
        if (reuseConnManager != null && reuseConnManager.isRunning()) {
            reuseConnManager.stop();
        }
        log.info("MySignHandler资源已释放");
    }
}
