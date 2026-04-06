package org.hylee.phms.server.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminHealthGoalVO(
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
        Integer goalStatus,
        Long createdBy,
        String creatorAccount,
        String creatorName,
        LocalDateTime createdTime,
        LocalDateTime lastChangeTime
) {
}
