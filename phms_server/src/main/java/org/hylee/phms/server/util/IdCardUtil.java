package org.hylee.phms.server.util;

import java.time.LocalDate;
import java.time.Period;

public final class IdCardUtil {

    private IdCardUtil() {
    }

    public record ParseResult(LocalDate birthDate, Integer gender, Integer age) {
    }

    public static ParseResult parse(String idcard) {
        return parse(idcard, LocalDate.now());
    }

    public static ParseResult parse(String idcard, LocalDate today) {
        if (idcard == null) {
            throw new IllegalArgumentException("身份证号不能为空");
        }
        String value = idcard.trim().toUpperCase();
        if (value.matches("^\\d{17}[\\dX]$")) {
            return parse18(value, today);
        }
        if (value.matches("^\\d{15}$")) {
            return parse15(value, today);
        }
        throw new IllegalArgumentException("身份证号格式不正确");
    }

    private static ParseResult parse18(String value, LocalDate today) {
        if (!isValidCheckCode(value)) {
            throw new IllegalArgumentException("身份证号校验位错误");
        }
        LocalDate birthDate = parseBirthDate(value.substring(6, 14), "yyyyMMdd");
        int sequenceCode = Character.digit(value.charAt(16), 10);
        int gender = sequenceCode % 2 == 0 ? 2 : 1;
        int age = calculateAge(birthDate, today);
        return new ParseResult(birthDate, gender, age);
    }

    private static ParseResult parse15(String value, LocalDate today) {
        String birthRaw = "19" + value.substring(6, 12);
        LocalDate birthDate = parseBirthDate(birthRaw, "yyyyMMdd");
        int sequenceCode = Character.digit(value.charAt(14), 10);
        int gender = sequenceCode % 2 == 0 ? 2 : 1;
        int age = calculateAge(birthDate, today);
        return new ParseResult(birthDate, gender, age);
    }

    private static boolean isValidCheckCode(String value) {
        int[] weights = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        char[] codes = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            int digit = Character.digit(value.charAt(i), 10);
            if (digit < 0) {
                return false;
            }
            sum += digit * weights[i];
        }
        char checkCode = codes[sum % 11];
        return checkCode == value.charAt(17);
    }

    private static LocalDate parseBirthDate(String dateText, String patternHint) {
        try {
            int year = Integer.parseInt(dateText.substring(0, 4));
            int month = Integer.parseInt(dateText.substring(4, 6));
            int day = Integer.parseInt(dateText.substring(6, 8));
            return LocalDate.of(year, month, day);
        } catch (Exception ex) {
            throw new IllegalArgumentException("身份证出生日期不合法(" + patternHint + ")");
        }
    }

    private static int calculateAge(LocalDate birthDate, LocalDate today) {
        if (birthDate.isAfter(today)) {
            throw new IllegalArgumentException("身份证出生日期不合法");
        }
        return Period.between(birthDate, today).getYears();
    }
}
