package org.hylee.phms.server.context;

/**
 * 当前登录用户快照（从数据库用户表加载后写入，避免在业务代码中反复查库）。
 *
 * @param userId        用户主键
 * @param account       登录账号
 * @param nickname      昵称
 * @param realname      真实姓名
 * @param accountLevel  账号级别（如 1 表示管理员）
 * @param accountStatus 账号状态（正常/异常/禁用等，与库表约定一致）
 */
public record LoginUser(
        Long userId,
        String account,
        String nickname,
        String realname,
        Integer accountLevel,
        Integer accountStatus
) {

    public boolean isAdmin() {
        return accountLevel != null && accountLevel == 1;
    }
}
