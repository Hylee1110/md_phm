package org.hylee.phms.server.vo;

import java.time.LocalDateTime;

public record UserDataSourceVO(
        Long sourceId,
        String sourceName,
        String sourceType,
        Integer sourceStatus,
        String description,
        LocalDateTime lastSyncTime,
        Long metricCount,
        Long taskCount,
        LocalDateTime createdTime,
        LocalDateTime lastChangeTime
) {
}
