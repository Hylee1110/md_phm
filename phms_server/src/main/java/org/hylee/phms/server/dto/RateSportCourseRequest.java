package org.hylee.phms.server.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RateSportCourseRequest(
        @NotNull(message = "score is required")
        @Min(value = 1, message = "score must be >= 1")
        @Max(value = 5, message = "score must be <= 5")
        Integer score,

        @Size(max = 500, message = "comment length must be <= 500")
        String comment
) {
}
