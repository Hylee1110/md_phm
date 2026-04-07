package org.hylee.phms.pojo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模板餐次中的食材明细（用于推荐食谱结构，与 {@link MealItem} 区分场景）。
 *
 * @param itemId    明细主键
 * @param mealId    所属餐次/模板 ID
 * @param foodId    食物 ID
 * @param foodName  食物名称
 * @param amount    用量
 * @param unit      单位
 * @param kcal      热量
 * @param mealTime  关联餐次时间（展示/排序用）
 */
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
