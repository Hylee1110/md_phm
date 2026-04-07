package org.hylee.phms.server.service;

import org.hylee.phms.server.vo.AdminUserVO;

import java.util.List;

/**
 * 管理端用户管理服务。
 * <p>
 * 面向普通用户列表与账号状态维护；不包含管理员自身账号的敏感变更逻辑。
 */
public interface AdminUserService {

    /** 查询普通用户列表，keyword 可为空表示不过滤。 */
    List<AdminUserVO> listNormalUsers(String keyword);

    /** 更新指定用户的账号状态（正常/异常/禁用等）。 */
    void updateAccountStatus(Long userId, Integer accountStatus);
}
