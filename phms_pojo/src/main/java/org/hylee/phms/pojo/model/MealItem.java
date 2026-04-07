package org.hylee.phms.pojo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 饮食记录中的明细项（单种食材及用量）。
 *
 * @param itemId        明细主键
 * @param foodId        食物 ID（可为空）
 * @param foodName      食物名称
 * @param amount        用量数值
 * @param unit          用量单位
 * @param kcal          该明细热量（可为空）
 * @param createdTime   创建时间
 * @param lastChangeTime 最后修改时间
 */
public record MealItem(
        Long itemId,
        Long foodId,
        String foodName,
        BigDecimal amount,
        String unit,
        BigDecimal kcal,
        LocalDateTime createdTime,
        LocalDateTime lastChangeTime
) {
}
