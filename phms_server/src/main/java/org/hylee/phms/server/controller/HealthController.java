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

@Validated
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final HealthDataService healthDataService;

    public HealthController(HealthDataService healthDataService) {
        this.healthDataService = healthDataService;
    }

    @GetMapping("/profile")
    public ApiResponse<ProfileDetailVO> profile() {
        return ApiResponse.success(healthDataService.getProfileDetail());
    }

    @GetMapping("/profile/idcard-recognition")
    public ApiResponse<IdCardRecognitionVO> recognizeByIdCard(
            @RequestParam(name = "idcard") @NotBlank(message = "身份证号不能为空") String idcard) {
        return ApiResponse.success(healthDataService.recognizeByIdCard(idcard));
    }

    @PutMapping("/profile")
    public ApiResponse<ProfileDetailVO> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success("保存成功", healthDataService.updateProfile(request));
    }

    @GetMapping("/dashboard")
    public ApiResponse<DashboardSummary> dashboard() {
        return ApiResponse.success(healthDataService.getDashboardSummary());
    }

    @GetMapping("/metrics")
    public ApiResponse<List<HealthMetric>> metrics(
            @RequestParam(name = "days", defaultValue = "7")
            @Min(value = 1, message = "days 不能小于 1")
            @Max(value = 30, message = "days 不能大于 30")
            Integer days) {
        return ApiResponse.success(healthDataService.getRecentMetrics(days));
    }
}
