package org.hylee.phms.server.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AdminSaveSportCourseRequest(
        @NotBlank(message = "name is required")
        @Size(max = 64, message = "name length must be <= 64")
        String name,

        @Size(max = 512, message = "coverUrl length must be <= 512")
        String coverUrl,

        @Size(max = 255, message = "summary length must be <= 255")
        String summary,

        String description,

        @NotNull(message = "recommendDurationMin is required")
        @Min(value = 1, message = "recommendDurationMin must be >= 1")
        @Max(value = 600, message = "recommendDurationMin must be <= 600")
        Integer recommendDurationMin,

        @NotNull(message = "caloriesPerHour is required")
        @Min(value = 0, message = "caloriesPerHour must be >= 0")
        @Max(value = 5000, message = "caloriesPerHour must be <= 5000")
        Integer caloriesPerHour,

        @NotNull(message = "recommendFrequencyPerWeek is required")
        @Min(value = 1, message = "recommendFrequencyPerWeek must be >= 1")
        @Max(value = 14, message = "recommendFrequencyPerWeek must be <= 14")
        Integer recommendFrequencyPerWeek,

        @NotBlank(message = "level is required")
        @Pattern(
                regexp = "beginner|intermediate|advanced|all",
                message = "level must be one of beginner/intermediate/advanced/all"
        )
        String level,

        @NotBlank(message = "status is required")
        @Pattern(
                regexp = "draft|published|archived",
                message = "status must be one of draft/published/archived"
        )
        String status,

        Integer sortWeight,

        List<@Positive(message = "audienceIds must contain positive id") Long> audienceIds,

        List<@Positive(message = "equipmentIds must contain positive id") Long> equipmentIds,

        List<@Positive(message = "benefitIds must contain positive id") Long> benefitIds
) {
}
