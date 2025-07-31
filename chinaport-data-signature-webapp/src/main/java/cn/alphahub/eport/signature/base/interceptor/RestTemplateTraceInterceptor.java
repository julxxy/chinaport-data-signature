package cn.alphahub.eport.signature.base.interceptor;

import cn.alphahub.eport.signature.base.constant.FrameworkConstant;
import cn.alphahub.eport.signature.base.utils.TraceHelper;
import java.io.IOException;
import java.util.Objects;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * RestTemplate Trace Interceptor.
 *
 * @author Julian
 */
public class RestTemplateTraceInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {
        String traceId = Objects.toString(MDC.get(FrameworkConstant.DEFAULT_TRACE_ID), TraceHelper.getTraceId(null));
        request.getHeaders().set(FrameworkConstant.DEFAULT_TRACE_ID, traceId);
        return clientHttpRequestExecution.execute(request, body);
    }

}
