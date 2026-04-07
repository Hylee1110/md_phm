package org.hylee.phms.server.vo;

import java.time.LocalDateTime;

/**
 * 单条用户数据源配置展示对象。
 */
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
