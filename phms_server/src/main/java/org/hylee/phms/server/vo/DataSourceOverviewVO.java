package org.hylee.phms.server.vo;

public record DataSourceOverviewVO(
        Long totalSources,
        Long activeSources,
        Long importedMetricDays,
        Long importedExerciseRecords,
        Long totalTasks,
        SyncTaskVO latestTask
) {
}
