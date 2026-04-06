package org.hylee.phms.pojo.model;

import java.time.LocalDate;

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
