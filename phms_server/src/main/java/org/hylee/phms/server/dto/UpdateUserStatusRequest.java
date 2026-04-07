package org.hylee.phms.server.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 管理端修改普通用户账号状态请求体。
 */
public record UpdateUserStatusRequest(
        @NotNull(message = "账号状态不能为空")
        @Min(value = 0, message = "账号状态参数错误")
        @Max(value = 2, message = "账号状态参数错误")
        Integer accountStatus
) {
}
