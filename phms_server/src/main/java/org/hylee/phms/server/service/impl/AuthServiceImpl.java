package org.hylee.phms.server.service.impl;

import jakarta.servlet.http.HttpSession;
import org.hylee.phms.server.constant.SessionConstants;
import org.hylee.phms.server.dto.LoginRequest;
import org.hylee.phms.server.dto.RegisterRequest;
import org.hylee.phms.server.exception.BizException;
import org.hylee.phms.server.mapper.UserMapper;
import org.hylee.phms.server.persistence.UserDO;
import org.hylee.phms.server.service.AuthService;
import org.hylee.phms.server.vo.UserSessionVO;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * {@link org.hylee.phms.server.service.AuthService} 实现。
 * <p>
 * 注册写入用户表；登录校验密码与账号状态后把用户 ID 写入 session；登出销毁 session。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserSessionVO register(RegisterRequest request) {
        if (userMapper.selectByAccount(request.account()) != null) {
            throw new BizException(4001, "账号已存在");
        }

        UserDO userDO = new UserDO();
        userDO.setAccount(request.account());
        userDO.setPasswordHash(passwordEncoder.encode(request.password()));
        userDO.setNickname(defaultText(request.nickname(), request.account()));
        userDO.setRealname(request.realname());
        userDO.setIdcard(request.idcard());
        userDO.setGender(request.gender() == null ? 0 : request.gender());
        userDO.setAge(request.age());
        userDO.setAccountLevel(0);
        userDO.setAccountStatus(0);
        userMapper.insertUser(userDO);

        UserDO created = userMapper.selectById(userDO.getUserId());
        return toSessionVO(created);
    }

    @Override
    public UserSessionVO login(LoginRequest request, HttpSession session) {
        UserDO userDO = userMapper.selectByAccount(request.account());
        if (userDO == null || !passwordEncoder.matches(request.password(), userDO.getPasswordHash())) {
            throw new BizException(4012, "账号或密码错误", HttpStatus.UNAUTHORIZED);
        }
        if (userDO.getAccountStatus() != null && userDO.getAccountStatus() == 2) {
            throw new BizException(4032, "账号已禁用，无法登录", HttpStatus.FORBIDDEN);
        }
        session.setAttribute(SessionConstants.LOGIN_USER_ID, userDO.getUserId());
        return toSessionVO(userDO);
    }

    @Override
    public void logout(HttpSession session) {
        session.invalidate();
    }

    @Override
    public UserSessionVO currentUser(Long userId) {
        UserDO userDO = userMapper.selectById(userId);
        if (userDO == null) {
            throw new BizException(4011, "登录状态失效，请重新登录", HttpStatus.UNAUTHORIZED);
        }
        return toSessionVO(userDO);
    }

    private UserSessionVO toSessionVO(UserDO userDO) {
        return new UserSessionVO(
                userDO.getUserId(),
                userDO.getAccount(),
                userDO.getNickname(),
                userDO.getRealname(),
                userDO.getAccountLevel(),
                userDO.getAccountStatus(),
                userDO.getCreatedTime()
        );
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
