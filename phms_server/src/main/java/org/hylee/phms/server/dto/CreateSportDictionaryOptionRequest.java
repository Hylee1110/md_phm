package org.hylee.phms.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理端新增运动课程字典项请求体（受众/器械/收益共用）。
 */
public record CreateSportDictionaryOptionRequest(
        @NotBlank(message = "name is required")
        @Size(max = 32, message = "name length must be <= 32")
        String name
) {
}
