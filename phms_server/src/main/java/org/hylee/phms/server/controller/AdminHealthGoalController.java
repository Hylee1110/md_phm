package org.hylee.phms.server.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.hylee.phms.common.api.ApiResponse;
import org.hylee.phms.server.dto.AdminSaveHealthGoalRequest;
import org.hylee.phms.server.service.AdminHealthGoalService;
import org.hylee.phms.server.vo.AdminHealthGoalVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端-健康目标维护接口。
 * <p>
 * 提供健康目标的列表查询、创建与更新能力。
 */
@Validated
@RestController
@RequestMapping("/api/admin/health-goals")
public class AdminHealthGoalController {

    private final AdminHealthGoalService adminHealthGoalService;

    public AdminHealthGoalController(AdminHealthGoalService adminHealthGoalService) {
        this.adminHealthGoalService = adminHealthGoalService;
    }

    /**
     * 查询健康目标列表（支持关键字与状态过滤）。
     */
    @GetMapping
    public ApiResponse<List<AdminHealthGoalVO>> listGoals(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false)
            @Min(value = 0, message = "status must be 0 or 1")
            @Max(value = 1, message = "status must be 0 or 1")
            Integer status) {
        return ApiResponse.success(adminHealthGoalService.listGoals(keyword, status));
    }

    /**
     * 创建健康目标。
     */
    @PostMapping
    public ApiResponse<AdminHealthGoalVO> createGoal(@Valid @RequestBody AdminSaveHealthGoalRequest request) {
        return ApiResponse.success("created", adminHealthGoalService.createGoal(request));
    }

    /**
     * 更新健康目标。
     */
    @PutMapping("/{goalId}")
    public ApiResponse<AdminHealthGoalVO> updateGoal(
            @PathVariable("goalId")
            @Positive(message = "goalId must be positive")
            Long goalId,
            @Valid @RequestBody AdminSaveHealthGoalRequest request) {
        return ApiResponse.success("updated", adminHealthGoalService.updateGoal(goalId, request));
    }
}
