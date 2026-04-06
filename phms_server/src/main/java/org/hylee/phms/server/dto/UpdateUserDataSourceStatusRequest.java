package org.hylee.phms.server.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateUserDataSourceStatusRequest(
        @NotNull(message = "sourceStatus is required")
        @Min(value = 0, message = "sourceStatus must be between 0 and 2")
        @Max(value = 2, message = "sourceStatus must be between 0 and 2")
        Integer sourceStatus
) {
}
