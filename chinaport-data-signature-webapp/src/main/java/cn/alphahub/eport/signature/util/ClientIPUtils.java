package cn.alphahub.eport.signature.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 获取客户端的真实 IP 地址
 *
 * @author weasley
 * @since 1.1.0
 */
public final class ClientIPUtils {

    static final String UNKNOWN = "unknown";

    private ClientIPUtils() {
    }

    public static String getClientIP(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };
        String ip = null;
        for (String header : headers) {
            ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !UNKNOWN.equalsIgnoreCase(ip)) {
                break;
            }
        }
        // 兜底
        if (ip == null || ip.isEmpty() || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级反代时，X-Forwarded-For会有多个IP，取第一个非unknown的
        if (ip != null && ip.contains(",")) {
            for (String realIp : ip.split(",")) {
                realIp = realIp.trim();
                if (!realIp.isEmpty() && !UNKNOWN.equalsIgnoreCase(realIp)) {
                    return realIp;
                }
            }
        }
        return ip;
    }

}
