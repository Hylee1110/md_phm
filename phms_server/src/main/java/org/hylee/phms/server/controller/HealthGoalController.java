package org.hylee.phms.server.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.hylee.phms.common.api.ApiResponse;
import org.hylee.phms.server.dto.SelectHealthGoalRequest;
import org.hylee.phms.server.service.HealthGoalService;
import org.hylee.phms.server.vo.HealthGoalCardVO;
import org.hylee.phms.server.vo.UserHealthGoalVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 健康目标相关接口（普通用户）。
 * <p>
 * 包含：
 * <ul>
 *   <li>可选健康目标列表</li>
 *   <li>用户已选择的目标列表</li>
 *   <li>选择/取消目标</li>
 * </ul>
 */
@Validated
@RestController
@RequestMapping("/api/health")
public class HealthGoalController {

    private final HealthGoalService healthGoalService;

    public HealthGoalController(HealthGoalService healthGoalService) {
        this.healthGoalService = healthGoalService;
    }

    /**
     * 查询可选健康目标列表（支持关键字过滤）。
     */
    @GetMapping("/goals")
    public ApiResponse<List<HealthGoalCardVO>> listGoals(
            @RequestParam(name = "keyword", required = false) String keyword) {
        return ApiResponse.success(healthGoalService.listAvailableGoals(keyword));
    }

    /**
     * 查询当前用户的目标列表（可按状态过滤）。
     */
    @GetMapping("/user-goals")
    public ApiResponse<List<UserHealthGoalVO>> listUserGoals(
            @RequestParam(name = "status", required = false)
            @Min(value = 0, message = "status must be between 0 and 2")
            @Max(value = 2, message = "status must be between 0 and 2")
            Integer status) {
        return ApiResponse.success(healthGoalService.listUserGoals(status));
    }

    /**
     * 选择一个健康目标（可携带个性化配置入参）。
     */
    @PostMapping("/goals/{goalId}/select")
    public ApiResponse<UserHealthGoalVO> selectGoal(
            @PathVariable("goalId")
            @Positive(message = "goalId must be positive")
            Long goalId,
            @Valid @RequestBody(required = false) SelectHealthGoalRequest request) {
        return ApiResponse.success("selected", healthGoalService.selectGoal(goalId, request));
    }

    /**
     * 取消已选择的目标。
     */
    @DeleteMapping("/user-goals/{userGoalId}")
    public ApiResponse<Void> cancelGoal(
            @PathVariable("userGoalId")
            @Positive(message = "userGoalId must be positive")
            Long userGoalId) {
        healthGoalService.cancelGoal(userGoalId);
        return ApiResponse.success("cancelled", null);
    }
}
