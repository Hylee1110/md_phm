package org.hylee.phms.server.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MealRecommendationVO(
        Long recipeId,
        Long createdBy,
        String creatorAccount,
        String creatorName,
        Boolean adminRecommend,
        Boolean createdByCurrentUser,
        Boolean editable,
        Boolean deletable,
        Integer mealType,
        String foodName,
        BigDecimal portion,
        String unit,
        Integer calories,
        String imageUrl,
        String description,
        BigDecimal ratingAvg,
        Integer ratingCount,
        Integer userScore,
        LocalDateTime createdTime,
        LocalDateTime lastChangeTime
) {
}
