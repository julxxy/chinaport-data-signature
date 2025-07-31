package cn.alphahub.eport.signature.base.constant;

/**
 * 框架常量接口
 *
 * @author julian
 * @since 1.2.0
 */
public interface FrameworkConstant {

    /**
     * 项目的基础包作用域 .
     */
    String BASE_PACKAGE = "cn.alphahub";

    /**
     * 第三方请求头前缀 .
     */
    String THIRD_HEADER_TOKEN_PREFIX = "third-";

    /**
     * 默认的 trace id.
     */
    String TRACE_ID = "X-B3-TraceId";

    /**
     * /api 的请求前缀 .
     */
    String URL_API_PREFIX_API = "/api";

    /**
     * /rpc 的请求前缀 .
     */
    String URL_API_PREFIX_RPC = "/rpc";

    /**
     * 无需权限拦截 .
     */
    String URL_API_PREFIX_PUBLIC = "/api/public";
}
