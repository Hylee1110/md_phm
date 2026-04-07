package org.hylee.phms.server.mapper;

import org.apache.ibatis.annotations.Param;
import org.hylee.phms.server.persistence.MealItemDO;
import org.hylee.phms.server.persistence.MealItemTemplateDO;

import java.util.List;

/**
 * 饮食明细与食材模板查询数据访问（批量插入、按餐次/用户查询）。
 */
public interface MealItemMapper {

    int batchInsert(@Param("mealId") Long mealId, @Param("items") List<MealItemDO> items);

    List<MealItemDO> selectByMealId(@Param("mealId") Long mealId);

    List<MealItemDO> selectByMealIds(@Param("mealIds") List<Long> mealIds);

    List<MealItemDO> selectByItemIdsAndUser(@Param("itemIds") List<Long> itemIds,
                                            @Param("userId") Long userId);

    List<MealItemTemplateDO> selectRecentItemTemplates(@Param("userId") Long userId,
                                                       @Param("keyword") String keyword,
                                                       @Param("limit") Integer limit);

    List<MealItemTemplateDO> selectRecentItemTemplatesGlobal(@Param("keyword") String keyword,
                                                             @Param("limit") Integer limit);

    int deleteByMealId(@Param("mealId") Long mealId);
}
