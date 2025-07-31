package cn.alphahub.eport.signature.base.interceptor;


import cn.alphahub.eport.signature.base.constant.FrameworkConstant;
import cn.alphahub.eport.signature.base.utils.TraceHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Default Trace Interceptor.
 *
 * @author Julian
 * @version 1.2.0
 */
public class DefaultTraceInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String traceId = TraceHelper.getTraceId(request);
        MDC.put(FrameworkConstant.TRACE_ID, traceId);
        if (!response.containsHeader(FrameworkConstant.TRACE_ID)) {
            response.setHeader(FrameworkConstant.TRACE_ID, traceId);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        MDC.remove(FrameworkConstant.TRACE_ID);
    }

}
