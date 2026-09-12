package cn.alphahub.eport.signature.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 获取客户端的真实 IP 地址
 * <p>
 * 转发头(X-Forwarded-For 等)的解析交由 Tomcat {@code RemoteIpValve} 完成, 见 application.yml 中的
 * {@code server.forward-headers-strategy=native} 与 {@code server.tomcat.remoteip.*} 配置:
 * 仅当直连方是可信代理时才信任转发头, 因此这里直接使用 {@link HttpServletRequest#getRemoteAddr()} 即可,
 * 不再在应用层读取任何客户端可伪造的请求头.
 *
 * @author weasley
 * @since 1.1.0
 */
public final class ClientIPUtils {

    private ClientIPUtils() {
    }

    public static String getClientIP(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

}
