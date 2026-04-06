package org.hylee.phms.server.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SportCourseCardVO(
        Long id,
        String name,
        String coverUrl,
        String summary,
        String description,
        Integer recommendDurationMin,
        Integer caloriesPerHour,
        Integer recommendFrequencyPerWeek,
        String level,
        BigDecimal ratingAvg,
        Integer ratingCount,
        Integer userScore,
        List<String> audiences,
        List<String> equipments,
        List<String> benefits,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
