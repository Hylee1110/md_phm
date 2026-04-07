package org.hylee.phms.server.vo;

import java.time.LocalDateTime;

/**
 * 管理端普通用户列表行展示对象。
 */
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
