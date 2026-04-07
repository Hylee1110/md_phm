package org.hylee.phms.pojo.model;

import java.util.List;

/**
 * 仪表盘/健康概览汇总。
 *
 * @param profile       用户基础档案
 * @param latestMetric  最新一条健康指标
 * @param recentMetrics 最近一段时间的指标列表（用于趋势图）
 * @param advice        简要建议文案（由服务端生成）
 */
public record DashboardSummary(
        UserProfile profile,
        HealthMetric latestMetric,
        List<HealthMetric> recentMetrics,
        String advice
) {
}
