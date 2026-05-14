package org.hylee.phms.common.api;

import java.time.LocalDateTime;

/**
 * 统一 API 响应包装。
 * <p>
 * 约定：
 * <ul>
 *   <li>code：业务状态码（HTTP 状态码语义的业务化表达，成功默认 200）</li>
 *   <li>message：给前端/调用方的可读提示</li>
 *   <li>data：实际返回数据（失败时通常为 null）</li>
 *   <li>timestamp：服务端生成响应的时间戳，便于排查与对齐日志</li>
 * </ul>
 *
 * @param code      状态码
 * @param message   提示信息
 * @param data      业务数据
 * @param timestamp 响应时间
 */
public record ApiResponse<T>(int code, String message, T data, LocalDateTime timestamp) {

    /**
     * 成功响应（默认 message 为 "success"）。
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data, LocalDateTime.now());
    }

    /**
     * 成功响应（自定义 message）。
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data, LocalDateTime.now());
    }

    /**
     * 失败响应。
     */
    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null, LocalDateTime.now());
    }
}
