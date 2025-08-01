package cn.alphahub.eport.signature.base.utils;

import cn.alphahub.eport.signature.base.constant.FrameworkConstant;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.Nullable;

/**
 * Trace Helper for generating and retrieving trace IDs.
 *
 * @author julian
 */
public final class TraceHelper {

    private static final Logger log = LoggerFactory.getLogger(TraceHelper.class);

    private TraceHelper() {
    }

    public static String generateTraceId() {
        return RandomStringUtils.secure().nextAlphanumeric(10);
    }

    public static String getTraceId(@Nullable HttpServletRequest request) {
        String traceId = MDC.get(FrameworkConstant.TRACE_ID);
        if (traceId == null) {
            traceId = (request != null) ? Objects.toString(request.getHeader(FrameworkConstant.TRACE_ID), generateTraceId()) : generateTraceId();
        }
        log.trace("Trace ID: {}", traceId);
        return traceId;
    }
}
