package org.hylee.phms.server.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.hylee.phms.common.api.ApiResponse;
import org.hylee.phms.server.context.LoginUserHolder;
import org.hylee.phms.server.dto.LoginRequest;
import org.hylee.phms.server.dto.RegisterRequest;
import org.hylee.phms.server.service.AuthService;
import org.hylee.phms.server.vo.UserSessionVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证与会话相关接口。
 * <p>
 * 说明：
 * <ul>
 *   <li>登录态使用 {@link HttpSession} 保存</li>
 *   <li>登录用户上下文从 {@link LoginUserHolder} 获取（由拦截器写入/清理）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户注册。
     */
    @PostMapping("/register")
    public ApiResponse<UserSessionVO> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("注册成功", authService.register(request));
    }

    /**
     * 用户登录（创建/刷新会话）。
     */
    @PostMapping("/login")
    public ApiResponse<UserSessionVO> login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        return ApiResponse.success("登录成功", authService.login(request, session));
    }

    /**
     * 用户退出登录（清理会话）。
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpSession session) {
        authService.logout(session);
        return ApiResponse.success("退出成功", null);
    }

    /**
     * 获取当前登录用户信息（用于前端初始化、权限与状态判断）。
     */
    @GetMapping("/me")
    public ApiResponse<UserSessionVO> me() {
        Long userId = LoginUserHolder.getUserId();
        return ApiResponse.success(authService.currentUser(userId));
    }
}
