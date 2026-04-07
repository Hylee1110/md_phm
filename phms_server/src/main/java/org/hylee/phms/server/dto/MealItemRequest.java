package org.hylee.phms.server.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 饮食记录中的单条食材明细请求体。
 */
public record MealItemRequest(
        @Positive(message = "foodId must be positive")
        Long foodId,

        @NotBlank(message = "foodName is required")
        @Size(max = 128, message = "foodName length must be <= 128")
        String foodName,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.00", inclusive = false, message = "amount must be > 0")
        @Digits(integer = 8, fraction = 2, message = "amount must match DECIMAL(10,2)")
        BigDecimal amount,

        @NotBlank(message = "unit is required")
        @Size(max = 16, message = "unit length must be <= 16")
        String unit,

        @DecimalMin(value = "0.00", message = "kcal must be >= 0")
        @Digits(integer = 8, fraction = 2, message = "kcal must match DECIMAL(10,2)")
        BigDecimal kcal
) {
}
