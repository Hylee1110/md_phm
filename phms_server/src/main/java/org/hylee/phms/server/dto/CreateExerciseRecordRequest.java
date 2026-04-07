package org.hylee.phms.server.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 新增运动记录请求体。
 */
public record CreateExerciseRecordRequest(
        @Positive(message = "sportId must be positive")
        Long sportId,

        @Size(max = 64, message = "sportName length must be <= 64")
        String sportName,

        @NotNull(message = "recordTime is required")
        LocalDateTime recordTime,

        @NotNull(message = "durationMin is required")
        @Min(value = 1, message = "durationMin must be >= 1")
        @Max(value = 1440, message = "durationMin must be <= 1440")
        Integer durationMin,

        @DecimalMin(value = "0.00", message = "caloriesKcal must be >= 0")
        @Digits(integer = 8, fraction = 2, message = "caloriesKcal must match DECIMAL(10,2)")
        BigDecimal caloriesKcal,

        @Size(max = 255, message = "note length must be <= 255")
        String note,

        @Size(max = 32, message = "dataSource length must be <= 32")
        String dataSource,

        @Size(max = 128, message = "externalId length must be <= 128")
        String externalId
) {
}
