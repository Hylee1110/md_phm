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

public record CreateMealRecordRequest(
        @NotNull(message = "recipeId is required")
        @Positive(message = "recipeId must be positive")
        Long recipeId,

        @NotNull(message = "diningTime is required")
        LocalDateTime diningTime,

        @Min(value = 0, message = "mealType must be between 0 and 4")
        @Max(value = 4, message = "mealType must be between 0 and 4")
        Integer mealType,

        @NotNull(message = "intakeAmount is required")
        @DecimalMin(value = "0.00", inclusive = false, message = "intakeAmount must be > 0")
        @Digits(integer = 8, fraction = 2, message = "intakeAmount must match DECIMAL(10,2)")
        BigDecimal intakeAmount,

        @Size(max = 500, message = "remark length must be <= 500")
        String remark
) {
}
