package org.hylee.phms.server.service;

import org.hylee.phms.server.vo.AdminUserVO;

import java.util.List;

public interface AdminUserService {

    List<AdminUserVO> listNormalUsers(String keyword);

    void updateAccountStatus(Long userId, Integer accountStatus);
}
