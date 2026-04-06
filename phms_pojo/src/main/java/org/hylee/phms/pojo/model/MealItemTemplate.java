package org.hylee.phms.pojo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MealItemTemplate(
        Long itemId,
        Long mealId,
        Long foodId,
        String foodName,
        BigDecimal amount,
        String unit,
        BigDecimal kcal,
        LocalDateTime mealTime
) {
}
