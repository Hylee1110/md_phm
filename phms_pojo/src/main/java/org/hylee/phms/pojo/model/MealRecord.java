package org.hylee.phms.pojo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 饮食记录（一餐或一次录入）。
 *
 * @param recordId           记录主键
 * @param recipeId           关联推荐食谱 ID（可为空）
 * @param foodName           食物/餐次名称
 * @param mealType           餐次类型编码（早/午/晚等，与前端约定）
 * @param diningTime         用餐时间
 * @param intakeAmount       摄入量（与单位配合，可为空）
 * @param caloriesPer100g    每百克热量参考（可为空）
 * @param estimatedCalories  估算总热量（可为空）
 * @param remark             备注
 * @param createdTime        创建时间
 * @param lastChangeTime     最后修改时间
 */
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
