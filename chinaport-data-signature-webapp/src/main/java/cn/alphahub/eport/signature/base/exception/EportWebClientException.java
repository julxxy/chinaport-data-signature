package cn.alphahub.eport.signature.base.exception;

import java.io.Serial;
import lombok.Getter;

/**
 * The eport web client exception
 *
 * @author weasley
 * @version 1.1.0
 */
@Getter
public class EportWebClientException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 异常消息
     */
    private final String msg;
    /**
     * 错误码, 默认: 500
     */
    private int code = 500;

    public EportWebClientException(String msg) {
        super(msg);
        this.msg = msg;
    }

    public EportWebClientException(String msg, Throwable e) {
        super(msg, e);
        this.msg = msg;
    }

    public EportWebClientException(String msg, int code, Throwable e) {
        super(msg, e);
        this.msg = msg;
        this.code = code;
    }
}
