package org.hylee.phms.server.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HealthRecordVO(
        Long recordId,
        Long userGoalId,
        Long goalId,
        BigDecimal recordValue,
        String recordText,
        LocalDateTime recordTime,
        Integer recordSource,
        Integer evaluationResult,
        String remark,
        LocalDateTime createdTime,
        LocalDateTime lastChangeTime
) {
}
