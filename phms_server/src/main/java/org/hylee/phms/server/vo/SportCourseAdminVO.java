package org.hylee.phms.server.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端运动课程编辑/详情展示对象。
 */
public record SportCourseAdminVO(
        Long id,
        String name,
        String coverUrl,
        String summary,
        String description,
        Integer recommendDurationMin,
        Integer caloriesPerHour,
        Integer recommendFrequencyPerWeek,
        String level,
        String status,
        Integer sortWeight,
        BigDecimal ratingAvg,
        Integer ratingCount,
        List<Long> audienceIds,
        List<Long> equipmentIds,
        List<Long> benefitIds,
        List<String> audiences,
        List<String> equipments,
        List<String> benefits,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
