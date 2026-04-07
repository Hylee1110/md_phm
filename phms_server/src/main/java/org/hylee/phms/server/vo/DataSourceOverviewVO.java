package org.hylee.phms.server.vo;

/**
 * 用户数据源总览统计（数量、最近任务等）。
 */
public record DataSourceOverviewVO(
        Long totalSources,
        Long activeSources,
        Long importedMetricDays,
        Long importedExerciseRecords,
        Long totalTasks,
        SyncTaskVO latestTask
) {
}
