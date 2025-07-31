package cn.alphahub.eport.signature.config;

import cn.alphahub.eport.signature.base.domain.Result;
import cn.alphahub.eport.signature.base.utils.TraceHelper;
import cn.alphahub.eport.signature.util.ClientIPUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
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
        @SuppressWarnings("all")
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            RateLimiter limiter = ipRateLimiterManager.getRateLimiterForIp(request);
            // 首次访问，limiter 可能为 null，直接放行
            if (limiter == null || limiter.tryAcquire()) {
                return true;
            } else {
                log.warn("触发限流，客户端IP: {}", ClientIPUtils.getClientIP(request));
                response.setContentType("application/json;charset=utf-8");
                PrintWriter writer = response.getWriter();
                Result<Object> result = Result.error(TOO_MANY_REQUESTS.value(), TOO_MANY_REQUESTS.getReasonPhrase());
                result.setTraceId(TraceHelper.getTraceId(request));
                writer.println(toJson(result));
                writer.flush();
                writer.close();
                return false;
            }
        }
    }

    /**
     * IP限流管理器
     */
    @Component
    public static class IpRateLimiterManager {
        /**
         * 每个IP地址限流: permitsPerSecond 个请求/秒
         */
        private static final double permitsPerSecond = 10.0;

        /**
         * 存储每个 IP 的 RateLimiter
         */
        @SuppressWarnings("all")
        private final Cache<String, RateLimiter> ipRateLimiters;

        /**
         * 标记 IP 是否首次访问
         */
        private final Cache<String, Boolean> ipFirstVisitFlags;

        public IpRateLimiterManager() {
            ipRateLimiters = Caffeine.newBuilder()
                    .expireAfterWrite(1, TimeUnit.MINUTES)
                    .build();

            ipFirstVisitFlags = Caffeine.newBuilder()
                    .expireAfterWrite(2, TimeUnit.MINUTES)
                    .build();
        }

        @SuppressWarnings("all")
        public RateLimiter getRateLimiterForIp(HttpServletRequest request) {
            String clientIP = ClientIPUtils.getClientIP(request);

            // 如果是首次访问，记录并放行（返回 null）
            Boolean firstVisit = ipFirstVisitFlags.getIfPresent(clientIP);
            if (firstVisit == null) {
                ipFirstVisitFlags.put(clientIP, true);
                return null;
            }

            // 非首次访问，正常限流
            return ipRateLimiters.get(clientIP, this::createRateLimiter);
        }

        @SuppressWarnings("all")
        private RateLimiter createRateLimiter(String ip) {
            return RateLimiter.create(permitsPerSecond);
        }
    }
}
