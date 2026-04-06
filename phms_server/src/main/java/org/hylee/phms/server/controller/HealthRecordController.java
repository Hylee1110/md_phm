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

@Validated
@RestController
@RequestMapping("/api/health/user-goals/{userGoalId}/records")
public class HealthRecordController {

    private final HealthRecordService healthRecordService;

    public HealthRecordController(HealthRecordService healthRecordService) {
        this.healthRecordService = healthRecordService;
    }

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

    @PostMapping
    public ApiResponse<HealthRecordVO> createRecord(
            @PathVariable("userGoalId")
            @Positive(message = "userGoalId must be positive")
            Long userGoalId,
            @Valid @RequestBody CreateHealthRecordRequest request) {
        return ApiResponse.success("created", healthRecordService.createRecord(userGoalId, request));
    }
}
