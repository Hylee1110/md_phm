package org.hylee.phms.server.vo;

import java.time.LocalDate;

/**
 * 身份证号解析结果（辅助填表）。
 */
public record IdCardRecognitionVO(
        String idcard,
        Integer gender,
        Integer age,
        LocalDate birthDate
) {
}
