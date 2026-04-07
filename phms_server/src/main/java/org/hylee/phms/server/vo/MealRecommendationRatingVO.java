package org.hylee.phms.server.vo;

import java.math.BigDecimal;

/**
 * 食谱评分结果展示（聚合均值、次数与当前用户打分）。
 */
public record MealRecommendationRatingVO(
        Long recipeId,
        BigDecimal ratingAvg,
        Integer ratingCount,
        Integer userScore
) {
}
