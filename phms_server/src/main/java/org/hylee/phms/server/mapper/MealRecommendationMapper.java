package org.hylee.phms.server.mapper;

import org.apache.ibatis.annotations.Param;
import org.hylee.phms.server.persistence.MealRecommendationDO;

import java.util.List;

/**
 * 饮食推荐（食谱）数据访问，含列表筛选与软删除。
 */
public interface MealRecommendationMapper {

    int insert(MealRecommendationDO recommendationDO);

    int updateById(MealRecommendationDO recommendationDO);

    List<MealRecommendationDO> selectRecommendations(@Param("userId") Long userId,
                                                     @Param("isAdmin") boolean isAdmin,
                                                     @Param("keyword") String keyword,
                                                     @Param("mealType") Integer mealType,
                                                     @Param("limit") Integer limit);

    MealRecommendationDO selectById(@Param("recipeId") Long recipeId,
                                    @Param("userId") Long userId);

    int softDeleteById(@Param("recipeId") Long recipeId);
}
