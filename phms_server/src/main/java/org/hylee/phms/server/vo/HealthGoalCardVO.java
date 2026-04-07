package org.hylee.phms.server.vo;

import java.math.BigDecimal;

/**
 * 可选健康目标卡片（含是否已选及用户侧目标区间摘要）。
 */
public record HealthGoalCardVO(
        Long goalId,
        String goalCode,
        String goalName,
        String goalDescription,
        Integer metricType,
        String unit,
        BigDecimal defaultTargetMin,
        BigDecimal defaultTargetMax,
        String defaultTargetText,
        Integer sortNo,
        Boolean selected,
        Long userGoalId,
        Integer userGoalStatus,
        BigDecimal targetMin,
        BigDecimal targetMax,
        String targetText
) {
}
