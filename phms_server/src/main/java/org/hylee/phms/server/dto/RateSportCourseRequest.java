package org.hylee.phms.server.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 对运动课程评分请求体（星级 + 可选短评）。
 */
public record RateSportCourseRequest(
        @NotNull(message = "score is required")
        @Min(value = 1, message = "score must be >= 1")
        @Max(value = 5, message = "score must be <= 5")
        Integer score,

        @Size(max = 500, message = "comment length must be <= 500")
        String comment
) {
}
