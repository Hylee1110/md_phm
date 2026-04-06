package org.hylee.phms.server.vo;

import java.math.BigDecimal;

public record MealRecommendationRatingVO(
        Long recipeId,
        BigDecimal ratingAvg,
        Integer ratingCount,
        Integer userScore
) {
}
