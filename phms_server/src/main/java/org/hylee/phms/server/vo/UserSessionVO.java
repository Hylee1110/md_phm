package org.hylee.phms.server.vo;

import java.time.LocalDateTime;

/**
 * 登录用户会话信息（返回给前端的当前用户摘要）。
 */
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
