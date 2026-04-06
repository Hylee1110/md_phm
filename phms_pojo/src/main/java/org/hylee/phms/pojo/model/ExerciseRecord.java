package org.hylee.phms.pojo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
