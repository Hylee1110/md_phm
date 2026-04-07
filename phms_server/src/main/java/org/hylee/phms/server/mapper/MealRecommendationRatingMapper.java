package org.hylee.phms.server.mapper;

import org.apache.ibatis.annotations.Param;
import org.hylee.phms.server.persistence.MealRecommendationRatingDO;

/**
 * 食谱评分流水与食谱侧聚合字段刷新。
 */
public interface MealRecommendationRatingMapper {

    int upsertRatingLog(@Param("recipeId") Long recipeId,
                        @Param("userId") Long userId,
                        @Param("score") Integer score);

    int refreshRecipeRating(@Param("recipeId") Long recipeId);

    MealRecommendationRatingDO selectByRecipeAndUser(@Param("recipeId") Long recipeId,
                                                     @Param("userId") Long userId);
}
