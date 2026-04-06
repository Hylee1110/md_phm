package org.hylee.phms.server.vo;

import java.time.LocalDateTime;

public record UserSessionVO(
        Long userId,
        String account,
        String nickname,
        String realname,
        Integer accountLevel,
        Integer accountStatus,
        LocalDateTime createdTime
) {
}
