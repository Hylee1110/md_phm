package org.hylee.phms.server.vo;

import java.time.LocalDate;

public record IdCardRecognitionVO(
        String idcard,
        Integer gender,
        Integer age,
        LocalDate birthDate
) {
}
