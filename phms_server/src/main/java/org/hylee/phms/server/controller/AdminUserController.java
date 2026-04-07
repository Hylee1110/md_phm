package org.hylee.phms.server.controller;

import jakarta.validation.Valid;
import org.hylee.phms.common.api.ApiResponse;
import org.hylee.phms.server.dto.UpdateUserStatusRequest;
import org.hylee.phms.server.service.AdminUserService;
import org.hylee.phms.server.vo.AdminUserVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端-用户管理接口。
 * <p>
 * 提供普通用户列表查询与账号状态维护能力。
 * 访问路径以 {@code /api/admin} 开头，通常会被鉴权拦截器要求管理员权限。
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * 查询普通用户列表（支持关键字模糊检索）。
     */
    @GetMapping
    public ApiResponse<List<AdminUserVO>> listUsers(
            @RequestParam(name = "keyword", required = false) String keyword) {
        return ApiResponse.success(adminUserService.listNormalUsers(keyword));
    }

    /**
     * 更新指定用户的账号状态（正常/异常/禁用等）。
     */
    @PutMapping("/{userId}/status")
    public ApiResponse<Void> updateUserStatus(@PathVariable("userId") Long userId,
                                              @Valid @RequestBody UpdateUserStatusRequest request) {
        adminUserService.updateAccountStatus(userId, request.accountStatus());
        return ApiResponse.success("状态更新成功", null);
    }
}
