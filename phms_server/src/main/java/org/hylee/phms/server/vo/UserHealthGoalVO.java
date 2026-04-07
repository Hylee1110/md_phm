package org.hylee.phms.server.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户已选健康目标详情展示对象。
 */
public record UserHealthGoalVO(
        Long userGoalId,
        Long goalId,
        String goalCode,
        String goalName,
        String goalDescription,
        Integer metricType,
        String unit,
        BigDecimal targetMin,
        BigDecimal targetMax,
        String targetText,
        LocalDate startDate,
        LocalDate endDate,
        Integer userGoalStatus,
        Integer recordCount,
        BigDecimal latestRecordValue,
        String latestRecordText,
        Integer latestEvaluationResult,
        LocalDateTime latestRecordTime,
        LocalDateTime createdTime,
        LocalDateTime lastChangeTime
) {
}
