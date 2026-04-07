package org.hylee.phms.server.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.hylee.phms.common.api.ApiResponse;
import org.hylee.phms.server.dto.CreateHealthRecordRequest;
import org.hylee.phms.server.service.HealthRecordService;
import org.hylee.phms.server.vo.HealthRecordVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 健康打卡/记录接口。
 * <p>
 * 记录与“用户目标（userGoalId）”关联，用于某个目标下的持续打卡与趋势追踪。
 */
@Validated
@RestController
@RequestMapping("/api/health/user-goals/{userGoalId}/records")
public class HealthRecordController {

    private final HealthRecordService healthRecordService;

    public HealthRecordController(HealthRecordService healthRecordService) {
        this.healthRecordService = healthRecordService;
    }

    /**
     * 查询指定用户目标下的打卡记录列表。
     *
     * @param userGoalId 用户目标 ID
     * @param rangeDays  取最近 N 天范围（可为空表示默认范围）
     * @param limit      最大返回条数
     */
    @GetMapping
    public ApiResponse<List<HealthRecordVO>> listRecords(
            @PathVariable("userGoalId")
            @Positive(message = "userGoalId must be positive")
            Long userGoalId,
            @RequestParam(name = "rangeDays", required = false)
            @Min(value = 1, message = "rangeDays must be >= 1")
            @Max(value = 3650, message = "rangeDays must be <= 3650")
            Integer rangeDays,
            @RequestParam(name = "limit", defaultValue = "400")
            @Min(value = 1, message = "limit must be >= 1")
            @Max(value = 1000, message = "limit must be <= 1000")
            Integer limit) {
        return ApiResponse.success(healthRecordService.listRecords(userGoalId, rangeDays, limit));
    }

    /**
     * 在指定用户目标下新增一条打卡记录。
     */
    @PostMapping
    public ApiResponse<HealthRecordVO> createRecord(
            @PathVariable("userGoalId")
            @Positive(message = "userGoalId must be positive")
            Long userGoalId,
            @Valid @RequestBody CreateHealthRecordRequest request) {
        return ApiResponse.success("created", healthRecordService.createRecord(userGoalId, request));
    }
}
