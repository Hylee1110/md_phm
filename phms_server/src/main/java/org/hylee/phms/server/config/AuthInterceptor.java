package org.hylee.phms.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.hylee.phms.common.api.ApiResponse;
import org.hylee.phms.server.constant.SessionConstants;
import org.hylee.phms.server.context.LoginUser;
import org.hylee.phms.server.context.LoginUserHolder;
import org.hylee.phms.server.mapper.UserMapper;
import org.hylee.phms.server.persistence.UserDO;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(UserMapper userMapper, ObjectMapper objectMapper) {
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        if (isPublicPath(path)) {
            return true;
        }
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            writeError(response, HttpStatus.UNAUTHORIZED, 4010, "请先登录");
            return false;
        }

        Object userIdAttr = session.getAttribute(SessionConstants.LOGIN_USER_ID);
        if (userIdAttr == null) {
            writeError(response, HttpStatus.UNAUTHORIZED, 4010, "请先登录");
            return false;
        }

        Long userId = Long.valueOf(String.valueOf(userIdAttr));
        UserDO userDO = userMapper.selectById(userId);
        if (userDO == null) {
            session.invalidate();
            writeError(response, HttpStatus.UNAUTHORIZED, 4011, "登录状态失效，请重新登录");
            return false;
        }

        if (userDO.getAccountStatus() != null && userDO.getAccountStatus() == 2) {
            session.invalidate();
            writeError(response, HttpStatus.FORBIDDEN, 4032, "账号已禁用，无法访问");
            return false;
        }

        LoginUser loginUser = new LoginUser(
                userDO.getUserId(),
                userDO.getAccount(),
                userDO.getNickname(),
                userDO.getRealname(),
                userDO.getAccountLevel(),
                userDO.getAccountStatus()
        );

        if (path.startsWith("/api/admin") && !loginUser.isAdmin()) {
            writeError(response, HttpStatus.FORBIDDEN, 4031, "无管理员权限");
            return false;
        }

        boolean isWriteRequest = !HttpMethod.GET.matches(request.getMethod())
                && !HttpMethod.HEAD.matches(request.getMethod())
                && !HttpMethod.OPTIONS.matches(request.getMethod());
        if (!loginUser.isAdmin()
                && loginUser.accountStatus() != null
                && loginUser.accountStatus() == 1
                && isWriteRequest) {
            writeError(response, HttpStatus.LOCKED, 4231, "账号状态异常，仅允许只读操作");
            return false;
        }
        LoginUserHolder.set(loginUser);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LoginUserHolder.clear();
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/register")
                || path.startsWith("/error");
    }

    private void writeError(HttpServletResponse response, HttpStatus status, int code, String message) throws Exception {
        response.setStatus(status.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(code, message));
    }
}
