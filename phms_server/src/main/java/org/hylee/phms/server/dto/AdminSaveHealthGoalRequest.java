package org.hylee.phms.server.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 管理端创建/更新健康目标模板请求体。
 */
public record AdminSaveHealthGoalRequest(
        @NotBlank(message = "goalCode is required")
        @Size(max = 64, message = "goalCode length must be <= 64")
        String goalCode,

        @NotBlank(message = "goalName is required")
        @Size(max = 128, message = "goalName length must be <= 128")
        String goalName,

        @Size(max = 500, message = "goalDescription length must be <= 500")
        String goalDescription,

        @NotNull(message = "metricType is required")
        @Min(value = 1, message = "metricType must be between 1 and 3")
        @Max(value = 3, message = "metricType must be between 1 and 3")
        Integer metricType,

        @Size(max = 32, message = "unit length must be <= 32")
        String unit,

        @Digits(integer = 8, fraction = 2, message = "defaultTargetMin must match DECIMAL(10,2)")
        BigDecimal defaultTargetMin,

        @Digits(integer = 8, fraction = 2, message = "defaultTargetMax must match DECIMAL(10,2)")
        BigDecimal defaultTargetMax,

        @Size(max = 255, message = "defaultTargetText length must be <= 255")
        String defaultTargetText,

        Integer sortNo,

        @Min(value = 0, message = "goalStatus must be 0 or 1")
        @Max(value = 1, message = "goalStatus must be 0 or 1")
        Integer goalStatus
) {
}
