package org.hylee.phms.pojo.model;

import java.util.List;

public record UserProfile(
        Long userId,
        String name,
        Integer age,
        String gender,
        Double heightCm,
        Double weightKg,
        List<String> tags
) {
}
