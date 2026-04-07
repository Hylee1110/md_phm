package org.hylee.phms.pojo.model;

import java.time.LocalDate;

/**
 * 单日健康指标汇总（步数、睡眠、血压、静息心率、压力等）。
 *
 * @param measureDate       统计/测量日期
 * @param steps             步数
 * @param restingHeartRate  静息心率（次/分，可为空）
 * @param sleepHours        睡眠时长（小时，可为空）
 * @param systolic          收缩压（可为空）
 * @param diastolic         舒张压（可为空）
 * @param stressLevel       压力指数（可为空）
 * @param sourceType        数据来源类型（如 device/import）
 * @param sourceName        数据来源名称（展示用）
 */
public record HealthMetric(
        LocalDate measureDate,
        Integer steps,
        Integer restingHeartRate,
        Double sleepHours,
        Integer systolic,
        Integer diastolic,
        Integer stressLevel,
        String sourceType,
        String sourceName
) {
}
