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

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ApiResponse<List<AdminUserVO>> listUsers(
            @RequestParam(name = "keyword", required = false) String keyword) {
        return ApiResponse.success(adminUserService.listNormalUsers(keyword));
    }

    @PutMapping("/{userId}/status")
    public ApiResponse<Void> updateUserStatus(@PathVariable("userId") Long userId,
                                              @Valid @RequestBody UpdateUserStatusRequest request) {
        adminUserService.updateAccountStatus(userId, request.accountStatus());
        return ApiResponse.success("状态更新成功", null);
    }
}
