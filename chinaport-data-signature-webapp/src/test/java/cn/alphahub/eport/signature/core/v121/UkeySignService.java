package cn.alphahub.eport.signature.core.v121;

import cn.alphahub.eport.signature.config.UkeyProperties;
import cn.alphahub.eport.signature.core.notify.EmailNotifyStrategy;
import cn.alphahub.eport.signature.core.notify.model.EmailNotifyRecord;
import cn.alphahub.eport.signature.entity.SignRequest;
import cn.alphahub.eport.signature.entity.SignResult;
import cn.alphahub.eport.signature.entity.UkeyRequest;
import cn.alphahub.eport.signature.entity.UkeyResponse;
import cn.alphahub.eport.signature.entity.WebSocketWrapper;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * U-Key加签服务最终兼容版（适配所有Spring版本，移除getSession()依赖）
 */
@Data
@Slf4j
//@Service
@Validated
@Accessors(chain = true)
public class UkeySignService {

    private static final String TRACE_ID = FrameworkConstant.TRACE_ID;
    private static final long CONNECT_TIMEOUT_MS = 5000;    // 连接超时
    private static final long RESPONSE_TIMEOUT_MS = 3000;   // 响应超时
    private static final long HEARTBEAT_INTERVAL_SEC = 30;  // 心跳间隔
    private static final long RECONNECT_RETRY_DELAY_MS = 1000; // 重连重试间隔
    private static final int RECONNECT_MAX_RETRY = 5;       // 重连最大重试次数
    // 核心组件
    private final WebSocketConnectionManager wsConnManager;
    private final AtomicReference<WebSocketSession> sessionRef = new AtomicReference<>(); // 缓存Session
    private final Map<String, CompletableFuture<UkeyResponse.Args>> requestFutureMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final Object connectLock = new Object();
    // 配置常量（建议通过配置文件注入）
    @Autowired
    private UkeyProperties ukeyProperties;
    /**
     * 电子口岸u-key加签失败通知策略
     */
    @Autowired
    @Qualifier(EmailNotifyStrategy.NAME)
    private EmailNotifyStrategy emailNotifyStrategy;

    // 初始化
    public UkeySignService(UkeyProperties ukeyProperties, EmailNotifyStrategy emailNotifyStrategy) {
        this.ukeyProperties = ukeyProperties;
        this.emailNotifyStrategy = emailNotifyStrategy;
        this.wsConnManager = createWebSocketManager();
        startHeartbeat(); // 启动心跳
    }

    /**
     * 创建WebSocket管理器（完全兼容低版本Spring）
     * 核心：自定义Handler缓存Session，替代getSession()
     */
    private WebSocketConnectionManager createWebSocketManager() {
        // 1. 配置WebSocket客户端（设置超时）
        StandardWebSocketClient webSocketClient = new StandardWebSocketClient();
        webSocketClient.setUserProperties(Map.of(
                "javax.websocket.client.connect.timeout", CONNECT_TIMEOUT_MS,
                "org.apache.tomcat.websocket.connect.timeout", CONNECT_TIMEOUT_MS
        ));

        // 2. 自定义Handler（缓存Session + 处理消息）
        TextWebSocketHandler customHandler = new TextWebSocketHandler() {
            // 连接建立时缓存Session
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                sessionRef.set(session); // 缓存Session实例
                isConnected.set(true);
                log.info("WebSocket连接成功，sessionId:{}", session.getId());
                super.afterConnectionEstablished(session);
            }

            // 处理响应消息
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                try {
                    MDC.put(TRACE_ID, UUID.randomUUID().toString()); // 补全TRACE_ID
                    String payload = message.getPayload();
                    // 严格非空校验
                    UkeyResponse response = JSONUtil.toBean(payload, UkeyResponse.class, true);
                    if (response == null || response.get_id() == null || response.get_args() == null) {
                        log.error("U-Key响应格式异常，payload:{}", payload);
                        return;
                    }

                    // 匹配请求并完成Future
                    String requestId = response.get_id().toString();
                    CompletableFuture<UkeyResponse.Args> future = requestFutureMap.remove(requestId);
                    if (future == null) {
                        log.warn("未找到匹配的U-Key请求，id:{}", requestId);
                        return;
                    }

                    // 处理结果
                    UkeyResponse.Args args = response.get_args();
                    if (Boolean.FALSE.equals(args.getResult())) {
                        String errorMsg = Objects.requireNonNullElse(args.getError().toString(), "未知错误");
                        log.error("U-Key加签失败 | id={}, error={}", requestId, errorMsg);
                        sendErrorNotify(response, errorMsg);
                        future.completeExceptionally(new UkeySignException(errorMsg));
                    } else {
                        log.info("U-Key加签成功 | id={}, data={}", requestId, args.getData());
                        future.complete(args);
                    }
                } catch (Exception e) {
                    log.error("处理U-Key响应异常", e);
                    failAllPendingFuture(new UkeySignException("响应处理异常", e));
                } finally {
                    MDC.remove(TRACE_ID);
                }
            }

            // 传输异常处理
            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                log.error("WebSocket传输异常 | sessionId={}", session.getId(), exception);
                sessionRef.set(null); // 清空失效Session
                isConnected.set(false);
                failAllPendingFuture(new UkeyConnectionException("传输异常", exception));
                super.handleTransportError(session, exception);
            }

            // 连接关闭处理
            @Override
            public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
                log.warn("WebSocket连接关闭 | sessionId={}, status={}", session.getId(), status);
                sessionRef.set(null); // 清空失效Session
                isConnected.set(false);
                failAllPendingFuture(new UkeyConnectionException("连接关闭，status:" + status));
                super.afterConnectionClosed(session, status);
            }
        };

        // 3. 创建连接管理器（仅使用通用方法，兼容所有版本）
        WebSocketConnectionManager manager = new WebSocketConnectionManager(
                webSocketClient, customHandler, ukeyProperties.getWsUrl()
        );
        manager.setAutoStartup(true); // 自动启动
        return manager;
    }

    /**
     * 获取有效WebSocketSession（替代getSession()）
     */
    private WebSocketSession getValidSession() {
        WebSocketSession session = sessionRef.get();
        // 校验Session是否有效
        if (session == null || !session.isOpen()) {
            return null;
        }
        return session;
    }

    /**
     * 心跳检测（保活+重连）
     */
    private void startHeartbeat() {
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            // 连接失效则重连
            if (!isConnected.get()) {
                reconnect();
                return;
            }

            // 发送心跳包
            WebSocketSession session = getValidSession();
            if (session != null) {
                try {
                    UkeyHeartbeatRequest heartbeat = new UkeyHeartbeatRequest();
                    heartbeat.setId(UUID.randomUUID().toString());
                    session.sendMessage(new TextMessage(JSONUtil.toJsonStr(heartbeat)));
                    log.debug("发送心跳包 | sessionId={}", session.getId());
                } catch (Exception e) {
                    log.error("发送心跳包异常", e);
                    isConnected.set(false);
                    sessionRef.set(null);
                }
            } else {
                isConnected.set(false);
            }
        }, 0, HEARTBEAT_INTERVAL_SEC, TimeUnit.SECONDS);
    }

    /**
     * 重连WebSocket（加锁避免并发重连）
     */
    private void reconnect() {
        synchronized (connectLock) {
            if (isConnected.get()) {
                return;
            }
            log.info("开始重连WebSocket...");

            try {
                // 停止旧连接
                if (wsConnManager.isRunning()) {
                    wsConnManager.stop();
                }
                // 启动新连接
                wsConnManager.start();

                // 等待连接建立（重试机制）
                int retry = 0;
                while (!isConnected.get() && retry < RECONNECT_MAX_RETRY) {
                    Thread.sleep(RECONNECT_RETRY_DELAY_MS);
                    retry++;
                }

                if (isConnected.get()) {
                    log.info("WebSocket重连成功（重试{}次）", retry);
                } else {
                    log.error("WebSocket重连失败（已重试{}次）", RECONNECT_MAX_RETRY);
                }
            } catch (Exception e) {
                log.error("WebSocket重连异常", e);
            }
        }
    }

    /**
     * 失败所有待处理请求
     */
    private void failAllPendingFuture(Throwable e) {
        if (requestFutureMap.isEmpty()) {
            return;
        }
        log.error("批量失败待处理请求 | 原因={}", e.getMessage());
        requestFutureMap.forEach((requestId, future) -> {
            future.completeExceptionally(e);
            log.error("请求失败 | id={}", requestId, e);
        });
        requestFutureMap.clear();
    }

    /**
     * 发送错误通知
     */
    private void sendErrorNotify(UkeyResponse response, String errorMsg) {
        try {
            WebSocketWrapper wrapper = new WebSocketWrapper();
            wrapper.setSessionId(MDC.get(TRACE_ID));
            wrapper.setPayload(JSONUtil.toJsonStr(response.get_args()));
            wrapper.setRequest(new SignRequest(response.get_id(), errorMsg));
            wrapper.setSignResult(new SignResult().setSuccess(false));

            emailNotifyStrategy.notify(new EmailNotifyRecord(wrapper, response));
        } catch (Exception e) {
            log.error("发送错误通知异常", e);
        }
    }

    // -------------------- 对外核心方法 --------------------

    /**
     * 同步获取U-Key加签结果（兼容原有调用方式）
     */
    public UkeyResponse.Args getUkeyResponseArgs(@NonNull UkeyRequest ukeyRequest) throws Exception {
        // 1. 参数校验
        if (ukeyRequest.get_id() == null) {
            throw new IllegalArgumentException("UkeyRequest的_id不能为空");
        }
        String requestId = String.valueOf(ukeyRequest.get_id());

        // 2. 确保连接可用
        if (!isConnected.get()) {
            reconnect();
            if (!isConnected.get()) {
                throw new UkeyConnectionException("WebSocket连接不可用");
            }
        }

        // 3. 创建Future并关联
        CompletableFuture<UkeyResponse.Args> future = new CompletableFuture<>();
        requestFutureMap.put(requestId, future);

        try {
            // 4. 获取有效Session并发送请求
            WebSocketSession session = getValidSession();
            if (session == null) {
                throw new UkeyConnectionException("WebSocket会话无效");
            }

            String requestJson = JSONUtil.toJsonStr(ukeyRequest);
            log.info("发送U-Key请求 | id={}, payload={}", requestId, requestJson);
            session.sendMessage(new TextMessage(requestJson));

            // 5. 等待响应（带超时）
            UkeyResponse.Args result = future.get(RESPONSE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (result == null) {
                throw new UkeySignException("U-Key返回空结果");
            }
            return result;
        } catch (TimeoutException e) {
            log.error("U-Key请求超时 | id={}", requestId);
            requestFutureMap.remove(requestId);
            throw new TimeoutException();
        } catch (ExecutionException e) {
            requestFutureMap.remove(requestId);
            Throwable cause = e.getCause();
            if (cause instanceof UkeySignException) {
                throw (UkeySignException) cause;
            } else if (cause instanceof UkeyConnectionException) {
                throw (UkeyConnectionException) cause;
            } else {
                throw new UkeySignException("加签处理失败: " + cause.getMessage(), cause);
            }
        } catch (Exception e) {
            requestFutureMap.remove(requestId);
            throw new UkeyConnectionException("请求发送失败", e);
        }
    }

    /**
     * 异步获取U-Key加签结果（推荐高并发场景）
     */
    public CompletableFuture<UkeyResponse.Args> getUkeyResponseArgsAsync(@NonNull UkeyRequest ukeyRequest) {
        // 1. 参数校验
        if (ukeyRequest.get_id() == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("UkeyRequest的_id不能为空"));
        }
        String requestId = ukeyRequest.get_id().toString();

        // 2. 连接校验
        if (!isConnected.get()) {
            reconnect();
            if (!isConnected.get()) {
                return CompletableFuture.failedFuture(new UkeyConnectionException("WebSocket连接不可用"));
            }
        }

        // 3. 创建Future
        CompletableFuture<UkeyResponse.Args> future = new CompletableFuture<>();
        requestFutureMap.put(requestId, future);

        // 4. 发送请求
        try {
            WebSocketSession session = getValidSession();
            if (session == null) {
                throw new UkeyConnectionException("WebSocket会话无效");
            }

            String requestJson = JSONUtil.toJsonStr(ukeyRequest);
            log.info("异步发送U-Key请求 | id={}, payload={}", requestId, requestJson);
            session.sendMessage(new TextMessage(requestJson));
        } catch (Exception e) {
            requestFutureMap.remove(requestId);
            return CompletableFuture.failedFuture(new UkeyConnectionException("请求发送失败", e));
        }

        // 5. 超时+异常处理
        return future.orTimeout(RESPONSE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> {
                    requestFutureMap.remove(requestId);
                    log.error("异步处理失败 | id={}", requestId, ex);
                    if (ex instanceof TimeoutException) {
                        try {
                            throw new Exception();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    } else if (ex instanceof UkeySignException) {
                        throw new CompletionException(ex);
                    } else {
                        try {
                            throw new Exception();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
    }

    /**
     * 销毁资源（Spring容器关闭时调用）
     */
    @PreDestroy
    public void destroy() {
        // 停止心跳线程池
        heartbeatExecutor.shutdown();
        try {
            if (!heartbeatExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                heartbeatExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            heartbeatExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // 停止WebSocket连接
        if (wsConnManager.isRunning()) {
            wsConnManager.stop();
        }

        // 清空缓存
        sessionRef.set(null);
        requestFutureMap.clear();
        log.info("U-Key加签服务资源已释放");
    }

    // -------------------- 异常定义 --------------------
    public static class UkeySignException extends Exception {
        public UkeySignException(String message) {
            super(message);
        }

        public UkeySignException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class UkeyConnectionException extends Exception {
        public UkeyConnectionException(String message) {
            super(message);
        }

        public UkeyConnectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }


    public static class UkeyHeartbeatRequest {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }


    public static class FrameworkConstant {
        public static final String TRACE_ID = "TRACE_ID";
    }
}
