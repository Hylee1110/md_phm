package org.hylee.phms.server.service;

import jakarta.servlet.http.HttpSession;
import org.hylee.phms.server.dto.LoginRequest;
import org.hylee.phms.server.dto.RegisterRequest;
import org.hylee.phms.server.vo.UserSessionVO;

public interface AuthService {

    UserSessionVO register(RegisterRequest request);

    UserSessionVO login(LoginRequest request, HttpSession session);

    void logout(HttpSession session);

    UserSessionVO currentUser(Long userId);
}
