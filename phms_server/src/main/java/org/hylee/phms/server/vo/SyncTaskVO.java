package org.hylee.phms.server.vo;

import java.time.LocalDateTime;

public record SyncTaskVO(
        Long taskId,
        Long sourceId,
        String sourceName,
        String taskType,
        Integer taskStatus,
        String fileName,
        String metricCategory,
        Integer totalCount,
        Integer insertCount,
        Integer updateCount,
        Integer failCount,
        String summaryMessage,
        LocalDateTime startedTime,
        LocalDateTime finishedTime
) {
}
