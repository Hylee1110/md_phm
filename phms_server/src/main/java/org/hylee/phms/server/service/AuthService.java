package org.hylee.phms.server.service;

import jakarta.servlet.http.HttpSession;
import org.hylee.phms.server.dto.LoginRequest;
import org.hylee.phms.server.dto.RegisterRequest;
import org.hylee.phms.server.vo.UserSessionVO;

/**
 * 认证与会话领域服务。
 * <p>
 * 负责用户注册、登录、登出及根据用户 ID 查询当前会话信息；与 {@link jakarta.servlet.http.HttpSession} 配合使用。
 */
public interface AuthService {

    /** 用户注册（不落 session，由调用方决定是否自动登录）。 */
    UserSessionVO register(RegisterRequest request);

    /** 用户登录：校验账号密码后写入 session。 */
    UserSessionVO login(LoginRequest request, HttpSession session);

    /** 退出登录：使 session 失效。 */
    void logout(HttpSession session);

    /** 根据用户 ID 加载当前用户信息（用于 /me 等接口）。 */
    UserSessionVO currentUser(Long userId);
}
