package org.hylee.phms.server.vo;

import java.math.BigDecimal;

/**
 * 运动课程评分结果展示对象。
 */
public record SportCourseRatingVO(
        Long courseId,
        BigDecimal ratingAvg,
        Integer ratingCount,
        Integer userScore
) {
}
