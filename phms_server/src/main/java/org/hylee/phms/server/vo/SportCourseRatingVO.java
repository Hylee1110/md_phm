package org.hylee.phms.server.vo;

import java.math.BigDecimal;

public record SportCourseRatingVO(
        Long courseId,
        BigDecimal ratingAvg,
        Integer ratingCount,
        Integer userScore
) {
}
