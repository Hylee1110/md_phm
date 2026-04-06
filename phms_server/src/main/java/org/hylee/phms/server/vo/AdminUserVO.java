package org.hylee.phms.server.vo;

import java.time.LocalDateTime;

public record AdminUserVO(
        Long userId,
        String account,
        String nickname,
        String realname,
        Integer gender,
        Integer age,
        Integer accountStatus,
        LocalDateTime createdTime,
        LocalDateTime lastChangeTime
) {
}
