package org.hylee.phms.pojo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MealItem(
        Long itemId,
        Long foodId,
        String foodName,
        BigDecimal amount,
        String unit,
        BigDecimal kcal,
        LocalDateTime createdTime,
        LocalDateTime lastChangeTime
) {
}
