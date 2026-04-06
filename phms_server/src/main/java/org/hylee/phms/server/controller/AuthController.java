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

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<UserSessionVO> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("注册成功", authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<UserSessionVO> login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        return ApiResponse.success("登录成功", authService.login(request, session));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpSession session) {
        authService.logout(session);
        return ApiResponse.success("退出成功", null);
    }

    @GetMapping("/me")
    public ApiResponse<UserSessionVO> me() {
        Long userId = LoginUserHolder.getUserId();
        return ApiResponse.success(authService.currentUser(userId));
    }
}
