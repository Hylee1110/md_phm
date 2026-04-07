package org.hylee.phms.server.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.hylee.phms.common.api.ApiResponse;
import org.hylee.phms.pojo.model.DashboardSummary;
import org.hylee.phms.pojo.model.HealthMetric;
import org.hylee.phms.server.dto.UpdateProfileRequest;
import org.hylee.phms.server.service.HealthDataService;
import org.hylee.phms.server.vo.IdCardRecognitionVO;
import org.hylee.phms.server.vo.ProfileDetailVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 健康数据相关接口。
 * <p>
 * 该控制器聚合了与“个人健康档案/健康概览/指标趋势”等相关的查询与更新能力，
 * 对外统一以 {@link ApiResponse} 结构返回，便于前端处理。
 */
@Validated
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final HealthDataService healthDataService;

    public HealthController(HealthDataService healthDataService) {
        this.healthDataService = healthDataService;
    }

    /**
     * 获取个人档案详情（用于“个人档案”页面展示）。
     */
    @GetMapping("/profile")
    public ApiResponse<ProfileDetailVO> profile() {
        return ApiResponse.success(healthDataService.getProfileDetail());
    }

    /**
     * 根据身份证号识别基础信息（用于辅助录入）。
     *
     * @param idcard 身份证号（不能为空）
     */
    @GetMapping("/profile/idcard-recognition")
    public ApiResponse<IdCardRecognitionVO> recognizeByIdCard(
            @RequestParam(name = "idcard") @NotBlank(message = "身份证号不能为空") String idcard) {
        return ApiResponse.success(healthDataService.recognizeByIdCard(idcard));
    }

    /**
     * 更新个人档案信息。
     */
    @PutMapping("/profile")
    public ApiResponse<ProfileDetailVO> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success("保存成功", healthDataService.updateProfile(request));
    }

    /**
     * 获取健康概览数据（用于首页/仪表盘）。
     */
    @GetMapping("/dashboard")
    public ApiResponse<DashboardSummary> dashboard() {
        return ApiResponse.success(healthDataService.getDashboardSummary());
    }

    /**
     * 获取最近 N 天的健康指标趋势数据。
     *
     * @param days 天数（1-30）
     */
    @GetMapping("/metrics")
    public ApiResponse<List<HealthMetric>> metrics(
            @RequestParam(name = "days", defaultValue = "7")
            @Min(value = 1, message = "days 不能小于 1")
            @Max(value = 30, message = "days 不能大于 30")
            Integer days) {
        return ApiResponse.success(healthDataService.getRecentMetrics(days));
    }
}
