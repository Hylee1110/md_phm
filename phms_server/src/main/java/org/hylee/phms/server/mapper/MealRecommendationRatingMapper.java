package org.hylee.phms.server.mapper;

import org.apache.ibatis.annotations.Param;
import org.hylee.phms.server.persistence.MealRecommendationRatingDO;

public interface MealRecommendationRatingMapper {

    int upsertRatingLog(@Param("recipeId") Long recipeId,
                        @Param("userId") Long userId,
                        @Param("score") Integer score);

    int refreshRecipeRating(@Param("recipeId") Long recipeId);

    MealRecommendationRatingDO selectByRecipeAndUser(@Param("recipeId") Long recipeId,
                                                     @Param("userId") Long userId);
}
