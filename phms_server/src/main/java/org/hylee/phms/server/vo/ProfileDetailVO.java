package org.hylee.phms.server.vo;

/**
 * 个人档案详情（页面展示用）。
 */
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
