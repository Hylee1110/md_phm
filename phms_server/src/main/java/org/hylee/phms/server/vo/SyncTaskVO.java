package org.hylee.phms.server.vo;

import java.time.LocalDateTime;

/**
 * 数据导入/同步任务展示对象。
 */
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
