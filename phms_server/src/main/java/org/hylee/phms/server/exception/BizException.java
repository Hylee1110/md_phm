package org.hylee.phms.server.exception;

import org.springframework.http.HttpStatus;

/**
 * 业务异常。
 * <p>
 * 同时携带业务错误码 {@link #getCode()} 与建议的 HTTP 状态 {@link #getHttpStatus()}，
 * 由 {@link GlobalExceptionHandler} 统一转换为 {@link org.hylee.phms.common.api.ApiResponse}。
 */
public class BizException extends RuntimeException {

    private final int code;
    private final HttpStatus httpStatus;

    public BizException(int code, String message) {
        this(code, message, HttpStatus.BAD_REQUEST);
    }

    public BizException(int code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
