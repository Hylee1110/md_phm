package org.hylee.phms.server.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 新建饮食推荐/食谱请求体。
 */
public record CreateMealRecommendationRequest(
        @NotBlank(message = "foodName is required")
        @Size(max = 100, message = "foodName length must be <= 100")
        String foodName,

        @Min(value = 0, message = "mealType must be between 0 and 4")
        @Max(value = 4, message = "mealType must be between 0 and 4")
        Integer mealType,

        @DecimalMin(value = "0.00", inclusive = false, message = "portion must be > 0")
        @Digits(integer = 8, fraction = 2, message = "portion must match DECIMAL(10,2)")
        BigDecimal portion,

        @Size(max = 20, message = "unit length must be <= 20")
        String unit,

        @Min(value = 0, message = "calories must be >= 0")
        Integer calories,

        @Size(max = 512, message = "imageUrl length must be <= 512")
        String imageUrl,

        @Size(max = 65535, message = "description length must be <= 65535")
        String description
) {
}
