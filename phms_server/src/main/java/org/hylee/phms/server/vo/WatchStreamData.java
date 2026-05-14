package org.hylee.phms.server.vo;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 手表实时上报的最新数据快照。
 */
public record WatchStreamData(
        Long userId,
        Long sourceId,
        Long taskId,
        Integer heartRate,
        Integer avgHeartRate,
        Integer restingHeartRate,
        Integer maxHeartRate,
        Integer stressLevel,
        Integer bodyBattery,
        Integer sleepDuration,
        Integer steps,
        Integer calories,
        Integer distanceCm,
        String device,
        Long timestamp,
        Long receivedAt,
        LocalDate measureDate,
        Double mappedSleepHours,
        Integer mappedRestingHeartRate,
        String sourceName,
        Map<String, String> rawQuery
) {

    public static WatchStreamData initial() {
        return new WatchStreamData(
                null,
                null,
                null,
                0,
                0,
                null,
                0,
                null,
                null,
                null,
                0,
                0,
                0,
                "Unknown",
                0L,
                0L,
                null,
                null,
                null,
                null,
                new LinkedHashMap<>()
        );
    }
}
