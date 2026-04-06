package org.hylee.phms.server.service.impl;

import org.hylee.phms.server.exception.BizException;
import org.hylee.phms.server.mapper.UserMapper;
import org.hylee.phms.server.persistence.UserDO;
import org.hylee.phms.server.service.AdminUserService;
import org.hylee.phms.server.vo.AdminUserVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final UserMapper userMapper;

    public AdminUserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public List<AdminUserVO> listNormalUsers(String keyword) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return userMapper.selectNormalUsers(normalizedKeyword).stream()
                .map(this::toAdminUserVO)
                .toList();
    }

    @Override
    public void updateAccountStatus(Long userId, Integer accountStatus) {
        if (accountStatus == null || accountStatus < 0 || accountStatus > 2) {
            throw new BizException(4002, "账号状态参数错误");
        }
        UserDO userDO = userMapper.selectById(userId);
        if (userDO == null) {
            throw new BizException(4041, "用户不存在");
        }
        if (userDO.getAccountLevel() != null && userDO.getAccountLevel() == 1) {
            throw new BizException(4003, "管理员账号状态不可在此接口修改");
        }
        userMapper.updateAccountStatus(userId, accountStatus);
    }

    private AdminUserVO toAdminUserVO(UserDO userDO) {
        return new AdminUserVO(
                userDO.getUserId(),
                userDO.getAccount(),
                userDO.getNickname(),
                userDO.getRealname(),
                userDO.getGender(),
                userDO.getAge(),
                userDO.getAccountStatus(),
                userDO.getCreatedTime(),
                userDO.getLastChangeTime()
        );
    }
}
