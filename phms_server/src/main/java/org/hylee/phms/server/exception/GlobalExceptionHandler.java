package org.hylee.phms.server.exception;

import jakarta.validation.ConstraintViolationException;
import org.hylee.phms.common.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理器。
 * <p>
 * 目标：
 * <ul>
 *   <li>把常见异常统一转换成 {@link ApiResponse} 格式</li>
 *   <li>为前端提供稳定的错误码与提示信息</li>
 * </ul>
 * 注意：这里的错误码是业务码（非 HTTP 状态码），HTTP 状态码仍会通过 {@link ResponseEntity} 返回。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 请求体参数校验失败（@Valid + @RequestBody）。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("请求参数校验失败");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(400, message));
    }

    /**
     * 请求参数校验失败（@Validated + @RequestParam 等）。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(400, ex.getMessage()));
    }

    /**
     * 业务异常（可携带业务码与 HTTP 状态码）。
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBizException(BizException ex) {
        return ResponseEntity.status(ex.getHttpStatus().value()).body(ApiResponse.fail(ex.getCode(), ex.getMessage()));
    }

    /**
     * 上传文件超过最大限制。
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.fail(413, "image must be <= 5MB"));
    }

    /**
     * 兜底异常处理：避免异常堆栈直接暴露给前端。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(500, "服务器内部错误: " + ex.getMessage()));
    }
}
