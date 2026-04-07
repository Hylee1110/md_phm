package org.hylee.phms.server.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 更新当前用户个人档案请求体。
 */
public record UpdateProfileRequest(
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
