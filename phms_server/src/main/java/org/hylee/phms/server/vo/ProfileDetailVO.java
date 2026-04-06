package org.hylee.phms.server.vo;

public record ProfileDetailVO(
        Long userId,
        String account,
        String nickname,
        String realname,
        String idcard,
        Integer gender,
        Integer age,
        Integer accountStatus
) {
}
