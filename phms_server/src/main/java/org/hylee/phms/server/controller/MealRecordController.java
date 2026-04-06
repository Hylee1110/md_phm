package org.hylee.phms.server.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.hylee.phms.common.api.ApiResponse;
import org.hylee.phms.pojo.model.MealRecord;
import org.hylee.phms.server.dto.CreateMealRecordRequest;
import org.hylee.phms.server.dto.UpdateMealRecordRequest;
import org.hylee.phms.server.service.MealRecordService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/health/meal-records")
public class MealRecordController {

    private final MealRecordService mealRecordService;

    public MealRecordController(MealRecordService mealRecordService) {
        this.mealRecordService = mealRecordService;
    }

    @GetMapping
    public ApiResponse<List<MealRecord>> list(
            @RequestParam(name = "startTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime,
            @RequestParam(name = "endTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endTime,
            @RequestParam(name = "limit", defaultValue = "50")
            @Min(value = 1, message = "limit must be >= 1")
            @Max(value = 200, message = "limit must be <= 200")
            Integer limit) {
        return ApiResponse.success(mealRecordService.listRecords(startTime, endTime, limit));
    }

    @GetMapping("/{recordId:\\d+}")
    public ApiResponse<MealRecord> detail(
            @PathVariable("recordId")
            @Positive(message = "recordId must be positive")
            Long recordId) {
        return ApiResponse.success(mealRecordService.getRecord(recordId));
    }

    @PostMapping
    public ApiResponse<MealRecord> create(@Valid @RequestBody CreateMealRecordRequest request) {
        return ApiResponse.success("created", mealRecordService.createRecord(request));
    }

    @PutMapping("/{recordId:\\d+}")
    public ApiResponse<MealRecord> update(
            @PathVariable("recordId")
            @Positive(message = "recordId must be positive")
            Long recordId,
            @Valid @RequestBody UpdateMealRecordRequest request) {
        return ApiResponse.success("updated", mealRecordService.updateRecord(recordId, request));
    }

    @DeleteMapping("/{recordId:\\d+}")
    public ApiResponse<Void> delete(
            @PathVariable("recordId")
            @Positive(message = "recordId must be positive")
            Long recordId) {
        mealRecordService.deleteRecord(recordId);
        return ApiResponse.success("deleted", null);
    }
}
