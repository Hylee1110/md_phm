package org.hylee.phms.server.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 在用户目标下新增健康打卡记录请求体。
 */
public record CreateHealthRecordRequest(
        @Digits(integer = 8, fraction = 2, message = "recordValue must match DECIMAL(10,2)")
        BigDecimal recordValue,

        @Size(max = 255, message = "recordText length must be <= 255")
        String recordText,

        @NotNull(message = "recordTime is required")
        LocalDateTime recordTime,

        @Min(value = 0, message = "recordSource must be between 0 and 2")
        @Max(value = 2, message = "recordSource must be between 0 and 2")
        Integer recordSource,

        @Size(max = 500, message = "remark length must be <= 500")
        String remark
) {
}
