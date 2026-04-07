package org.hylee.phms.pojo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 运动记录（一次运动会话）。
 *
 * @param recordId      记录主键
 * @param sportId       运动项目 ID（可为空）
 * @param sportName     运动项目名称
 * @param recordTime    运动发生时间
 * @param durationMin   时长（分钟）
 * @param caloriesKcal  消耗热量（千卡，可为空）
 * @param note          备注
 * @param dataSource    数据来源（manual/device/import 等）
 * @param externalId    外部系统唯一键（导入去重，可为空）
 * @param createdTime   创建时间
 * @param lastChangeTime 最后修改时间
 */
public record ExerciseRecord(
        Long recordId,
        Long sportId,
        String sportName,
        LocalDateTime recordTime,
        Integer durationMin,
        BigDecimal caloriesKcal,
        String note,
        String dataSource,
        String externalId,
        LocalDateTime createdTime,
        LocalDateTime lastChangeTime
) {
}
