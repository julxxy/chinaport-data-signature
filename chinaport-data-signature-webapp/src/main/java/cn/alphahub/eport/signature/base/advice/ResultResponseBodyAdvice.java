package cn.alphahub.eport.signature.base.advice;

import cn.alphahub.eport.signature.base.constant.FrameworkConstant;
import cn.alphahub.eport.signature.base.domain.AbstractResult;
import cn.alphahub.eport.signature.base.domain.Result;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;


/**
 * 全局响应体处理器, 用于统一处理响应体的格式.
 * <p>
 * 如果响应体为 null, 则返回一个 Result.ok(null) 的结果.
 * 如果响应体是 Result 类型且 traceId 为空, 则设置 traceId.
 *
 * @author Julian
 * @since 1.2.0
 */
@RestControllerAdvice(FrameworkConstant.BASE_PACKAGE)
public class ResultResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        String traceId = MDC.get(FrameworkConstant.DEFAULT_TRACE_ID);

        if (null == body) {
            Result<Object> result = Result.ok(null);
            result.setTraceId(traceId);
            return result;
        }

        if (body instanceof AbstractResult) {
            if (((AbstractResult<?>) body).getTraceId() == null) {
                ((AbstractResult<?>) body).setTraceId(traceId);
                return body;
            }
        }

        return body;
    }

}
