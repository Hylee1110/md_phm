package org.hylee.phms.server.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.hylee.phms.common.api.ApiResponse;
import org.hylee.phms.pojo.model.ExerciseRecord;
import org.hylee.phms.server.dto.CreateExerciseRecordRequest;
import org.hylee.phms.server.dto.UpdateExerciseRecordRequest;
import org.hylee.phms.server.service.ExerciseRecordService;
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
@RequestMapping("/api/health/exercise-records")
public class ExerciseRecordController {

    private final ExerciseRecordService exerciseRecordService;

    public ExerciseRecordController(ExerciseRecordService exerciseRecordService) {
        this.exerciseRecordService = exerciseRecordService;
    }

    @GetMapping
    public ApiResponse<List<ExerciseRecord>> list(
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
        return ApiResponse.success(exerciseRecordService.listRecords(startTime, endTime, limit));
    }

    @GetMapping("/{recordId}")
    public ApiResponse<ExerciseRecord> detail(
            @PathVariable("recordId")
            @Positive(message = "recordId must be positive")
            Long recordId) {
        return ApiResponse.success(exerciseRecordService.getRecord(recordId));
    }

    @PostMapping
    public ApiResponse<ExerciseRecord> create(@Valid @RequestBody CreateExerciseRecordRequest request) {
        return ApiResponse.success("created", exerciseRecordService.createRecord(request));
    }

    @PutMapping("/{recordId}")
    public ApiResponse<ExerciseRecord> update(
            @PathVariable("recordId")
            @Positive(message = "recordId must be positive")
            Long recordId,
            @Valid @RequestBody UpdateExerciseRecordRequest request) {
        return ApiResponse.success("updated", exerciseRecordService.updateRecord(recordId, request));
    }

    @DeleteMapping("/{recordId}")
    public ApiResponse<Void> delete(
            @PathVariable("recordId")
            @Positive(message = "recordId must be positive")
            Long recordId) {
        exerciseRecordService.deleteRecord(recordId);
        return ApiResponse.success("deleted", null);
    }
}
