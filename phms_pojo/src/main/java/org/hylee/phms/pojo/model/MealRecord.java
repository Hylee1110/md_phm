package org.hylee.phms.pojo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MealRecord(
        Long recordId,
        Long recipeId,
        String foodName,
        Integer mealType,
        LocalDateTime diningTime,
        BigDecimal intakeAmount,
        BigDecimal caloriesPer100g,
        BigDecimal estimatedCalories,
        String remark,
        LocalDateTime createdTime,
        LocalDateTime lastChangeTime
) {
}
