package org.hylee.phms.server.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 对食谱打星评分请求体（1–5 分）。
 */
public record RateMealRecommendationRequest(
        @NotNull(message = "score is required")
        @Min(value = 1, message = "score must be >= 1")
        @Max(value = 5, message = "score must be <= 5")
        Integer score
) {
}
