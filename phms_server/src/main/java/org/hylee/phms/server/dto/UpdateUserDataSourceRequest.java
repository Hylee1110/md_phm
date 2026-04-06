package org.hylee.phms.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserDataSourceRequest(
        @NotBlank(message = "sourceName is required")
        @Size(max = 64, message = "sourceName length must be <= 64")
        String sourceName,

        @NotBlank(message = "sourceType is required")
        @Size(max = 32, message = "sourceType length must be <= 32")
        String sourceType,

        @Size(max = 255, message = "description length must be <= 255")
        String description
) {
}
