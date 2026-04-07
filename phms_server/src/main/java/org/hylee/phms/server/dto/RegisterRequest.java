package org.hylee.phms.server.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户注册请求体（账号、密码与可选档案字段）。
 */
public record RegisterRequest(
        @NotBlank(message = "账号不能为空")
        @Size(min = 4, max = 32, message = "账号长度必须在 4 到 32 之间")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "账号仅支持字母、数字和下划线")
        String account,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 32, message = "密码长度必须在 6 到 32 之间")
        String password,

        @Size(max = 64, message = "昵称长度不能超过 64")
        String nickname,

        @Size(max = 64, message = "姓名长度不能超过 64")
        String realname,

        @Size(max = 32, message = "身份证号长度不能超过 32")
        String idcard,

        @Min(value = 0, message = "性别参数错误")
        @Max(value = 2, message = "性别参数错误")
        Integer gender,

        @Min(value = 0, message = "年龄不能小于 0")
        @Max(value = 130, message = "年龄不能大于 130")
        Integer age
) {
}
