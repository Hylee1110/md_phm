package org.hylee.phms.server.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 用户选择健康目标时的可选个性化参数（目标区间、文案与周期）。
 */
public record SelectHealthGoalRequest(
        @Digits(integer = 8, fraction = 2, message = "targetMin must match DECIMAL(10,2)")
        BigDecimal targetMin,

        @Digits(integer = 8, fraction = 2, message = "targetMax must match DECIMAL(10,2)")
        BigDecimal targetMax,

        @Size(max = 255, message = "targetText length must be <= 255")
        String targetText,

        LocalDate startDate,

        LocalDate endDate
) {
}
