package org.hylee.phms.server.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.hylee.phms.common.api.ApiResponse;
import org.hylee.phms.server.dto.CreateUserDataSourceRequest;
import org.hylee.phms.server.dto.UpdateUserDataSourceRequest;
import org.hylee.phms.server.dto.UpdateUserDataSourceStatusRequest;
import org.hylee.phms.server.service.DataSourceService;
import org.hylee.phms.server.vo.DataSourceOverviewVO;
import org.hylee.phms.server.vo.SyncTaskVO;
import org.hylee.phms.server.vo.UserDataSourceVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/health/data-sources")
public class DataSourceController {

    private final DataSourceService dataSourceService;

    public DataSourceController(DataSourceService dataSourceService) {
        this.dataSourceService = dataSourceService;
    }

    @GetMapping
    public ApiResponse<List<UserDataSourceVO>> listSources() {
        return ApiResponse.success(dataSourceService.listSources());
    }

    @PostMapping
    public ApiResponse<UserDataSourceVO> createSource(@Valid @RequestBody CreateUserDataSourceRequest request) {
        return ApiResponse.success("created", dataSourceService.createSource(request));
    }

    @PutMapping("/{sourceId}")
    public ApiResponse<UserDataSourceVO> updateSource(
            @PathVariable("sourceId")
            @Positive(message = "sourceId must be positive")
            Long sourceId,
            @Valid @RequestBody UpdateUserDataSourceRequest request) {
        return ApiResponse.success("updated", dataSourceService.updateSource(sourceId, request));
    }

    @PutMapping("/{sourceId}/status")
    public ApiResponse<UserDataSourceVO> updateStatus(
            @PathVariable("sourceId")
            @Positive(message = "sourceId must be positive")
            Long sourceId,
            @Valid @RequestBody UpdateUserDataSourceStatusRequest request) {
        return ApiResponse.success("updated", dataSourceService.updateSourceStatus(sourceId, request.sourceStatus()));
    }

    @DeleteMapping("/{sourceId}")
    public ApiResponse<Void> deleteSource(
            @PathVariable("sourceId")
            @Positive(message = "sourceId must be positive")
            Long sourceId) {
        dataSourceService.deleteSource(sourceId);
        return ApiResponse.success("deleted", null);
    }

    @PostMapping(value = "/{sourceId}/imports/health-metrics", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SyncTaskVO> importHealthMetrics(
            @PathVariable("sourceId")
            @Positive(message = "sourceId must be positive")
            Long sourceId,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success("imported", dataSourceService.importHealthMetrics(sourceId, file));
    }

    @PostMapping(value = "/{sourceId}/imports/exercise-records", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SyncTaskVO> importExerciseRecords(
            @PathVariable("sourceId")
            @Positive(message = "sourceId must be positive")
            Long sourceId,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success("imported", dataSourceService.importExerciseRecords(sourceId, file));
    }

    @GetMapping("/sync-tasks")
    public ApiResponse<List<SyncTaskVO>> listSyncTasks(
            @RequestParam(name = "sourceId", required = false) Long sourceId,
            @RequestParam(name = "limit", defaultValue = "10")
            @Min(value = 1, message = "limit must be >= 1")
            @Max(value = 50, message = "limit must be <= 50")
            Integer limit) {
        return ApiResponse.success(dataSourceService.listSyncTasks(sourceId, limit));
    }

    @GetMapping("/overview")
    public ApiResponse<DataSourceOverviewVO> getOverview() {
        return ApiResponse.success(dataSourceService.getOverview());
    }
}
