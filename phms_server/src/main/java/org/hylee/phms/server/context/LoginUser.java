package org.hylee.phms.server.context;

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
