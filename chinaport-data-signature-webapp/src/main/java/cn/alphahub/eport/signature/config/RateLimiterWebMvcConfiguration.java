package cn.alphahub.eport.signature.config;

import cn.alphahub.eport.signature.base.domain.Result;
import cn.alphahub.eport.signature.base.utils.TraceHelper;
import cn.alphahub.eport.signature.util.ClientIPUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static cn.alphahub.dtt.plus.util.JacksonUtil.toJson;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

/**
 * Rate Limiter Web Mvc Configuration
 *
 * @since 1.2.0
 */
@Slf4j
@Configuration
@AllArgsConstructor
public class RateLimiterWebMvcConfiguration implements WebMvcConfigurer {
    private final IpRateLimiterManager ipRateLimiterManager;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimiterInterceptor(ipRateLimiterManager))
                .addPathPatterns("/**")
                .excludePathPatterns("/rpc/**");
    }

    /**
     * 限流拦截器
     */
    @Slf4j
    public static class RateLimiterInterceptor implements HandlerInterceptor {
        private final IpRateLimiterManager ipRateLimiterManager;

        public RateLimiterInterceptor(IpRateLimiterManager ipRateLimiterManager) {
            this.ipRateLimiterManager = ipRateLimiterManager;
        }

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            ConsumptionProbe probe = ipRateLimiterManager.tryConsume(request);
            if (probe.isConsumed()) {
                return true;
            }
            log.warn("触发限流，客户端IP: {}", ClientIPUtils.getClientIP(request));
            long retryAfterSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
            response.setStatus(TOO_MANY_REQUESTS.value());
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
            response.setContentType("application/json;charset=utf-8");
            Result<Object> result = Result.error(TOO_MANY_REQUESTS.value(), TOO_MANY_REQUESTS.getReasonPhrase());
            result.setTraceId(TraceHelper.getTraceId(request));
            PrintWriter writer = response.getWriter();
            writer.println(toJson(result));
            writer.flush();
            return false;
        }
    }

    /**
     * IP限流管理器
     * <p>
     * 每个 IP 一个令牌桶: 容量 {@link #PERMITS_PER_SECOND}, 每秒补满; 新桶初始即为满桶, 允许新客户端第一秒内的突发请求.
     */
    @Component
    public static class IpRateLimiterManager {
        /**
         * 每个IP地址限流: PERMITS_PER_SECOND 个请求/秒
         */
        private static final long PERMITS_PER_SECOND = 10L;

        /**
         * 最多同时跟踪的 IP 数, 防止海量 IP 撑爆内存
         */
        private static final long MAX_TRACKED_IPS = 10_000L;

        /**
         * 存储每个 IP 的令牌桶, 一段时间不再访问后自动淘汰
         */
        private final Cache<String, Bucket> ipBuckets;

        public IpRateLimiterManager() {
            ipBuckets = Caffeine.newBuilder()
                    .maximumSize(MAX_TRACKED_IPS)
                    .expireAfterAccess(1, TimeUnit.MINUTES)
                    .build();
        }

        /**
         * 尝试为当前请求消耗一个令牌
         *
         * @return 消耗结果, {@link ConsumptionProbe#isConsumed()} 为 true 表示放行
         */
        public ConsumptionProbe tryConsume(HttpServletRequest request) {
            String clientIP = ClientIPUtils.getClientIP(request);
            Bucket bucket = ipBuckets.get(clientIP, this::createBucket);
            return bucket.tryConsumeAndReturnRemaining(1);
        }

        private Bucket createBucket(String ip) {
            Bandwidth limit = Bandwidth.builder()
                    .capacity(PERMITS_PER_SECOND)
                    .refillGreedy(PERMITS_PER_SECOND, Duration.ofSeconds(1))
                    .build();
            return Bucket.builder().addLimit(limit).build();
        }
    }
}
