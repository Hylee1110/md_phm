package org.hylee.phms.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSportDictionaryOptionRequest(
        @NotBlank(message = "name is required")
        @Size(max = 32, message = "name length must be <= 32")
        String name
) {
}
