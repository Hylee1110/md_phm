package org.hylee.phms.pojo.model;

import java.util.List;

public record DashboardSummary(
        UserProfile profile,
        HealthMetric latestMetric,
        List<HealthMetric> recentMetrics,
        String advice
) {
}
