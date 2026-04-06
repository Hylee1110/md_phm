package org.hylee.phms.server.repository;

import org.hylee.phms.pojo.model.HealthMetric;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class MockHealthDataRepository {

    private final List<HealthMetric> metricHistory = List.of(
            new HealthMetric(LocalDate.now().minusDays(6), 6950, 72, 6.3, 127, 81, 62, "mock", "Mock Seed"),
            new HealthMetric(LocalDate.now().minusDays(5), 8120, 70, 6.9, 124, 79, 58, "mock", "Mock Seed"),
            new HealthMetric(LocalDate.now().minusDays(4), 9300, 69, 7.2, 122, 78, 52, "mock", "Mock Seed"),
            new HealthMetric(LocalDate.now().minusDays(3), 10020, 68, 7.4, 121, 77, 49, "mock", "Mock Seed"),
            new HealthMetric(LocalDate.now().minusDays(2), 7860, 71, 6.8, 126, 80, 60, "mock", "Mock Seed"),
            new HealthMetric(LocalDate.now().minusDays(1), 8480, 70, 7.0, 124, 79, 56, "mock", "Mock Seed"),
            new HealthMetric(LocalDate.now(), 9060, 69, 7.3, 123, 78, 50, "mock", "Mock Seed")
    );

    public List<HealthMetric> listMetrics() {
        return metricHistory;
    }
}
