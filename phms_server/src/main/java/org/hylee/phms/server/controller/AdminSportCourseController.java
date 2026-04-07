package org.hylee.phms.server.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.hylee.phms.common.api.ApiResponse;
import org.hylee.phms.server.dto.AdminSaveSportCourseRequest;
import org.hylee.phms.server.dto.CreateSportDictionaryOptionRequest;
import org.hylee.phms.server.service.AdminSportCourseService;
import org.hylee.phms.server.vo.SportCourseAdminVO;
import org.hylee.phms.server.vo.SportCourseCoverUploadVO;
import org.hylee.phms.server.vo.SportCourseOptionsVO;
import org.hylee.phms.server.vo.SportDictionaryOptionVO;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 管理端-运动课程维护接口。
 * <p>
 * 包含：
 * <ul>
 *   <li>课程列表/详情查询</li>
 *   <li>课程创建/更新/删除</li>
 *   <li>课程封面上传</li>
 *   <li>字典选项维护（受众/器械/收益等）</li>
 * </ul>
 */
@Validated
@RestController
@RequestMapping("/api/admin/sport-courses")
public class AdminSportCourseController {

    private final AdminSportCourseService adminSportCourseService;

    public AdminSportCourseController(AdminSportCourseService adminSportCourseService) {
        this.adminSportCourseService = adminSportCourseService;
    }

    /**
     * 查询课程列表（支持关键字与状态过滤）。
     */
    @GetMapping
    public ApiResponse<List<SportCourseAdminVO>> listCourses(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) String status) {
        return ApiResponse.success(adminSportCourseService.listCourses(keyword, status));
    }

    /**
     * 获取课程相关选项（用于管理端表单下拉项等）。
     */
    @GetMapping("/options")
    public ApiResponse<SportCourseOptionsVO> options() {
        return ApiResponse.success(adminSportCourseService.getOptions());
    }

    /**
     * 获取单个课程详情（管理端编辑页）。
     */
    @GetMapping("/{courseId}")
    public ApiResponse<SportCourseAdminVO> getCourse(
            @PathVariable("courseId")
            @Positive(message = "courseId must be positive")
            Long courseId) {
        return ApiResponse.success(adminSportCourseService.getCourse(courseId));
    }

    /**
     * 新增“受众”字典选项。
     */
    @PostMapping("/options/audiences")
    public ApiResponse<SportDictionaryOptionVO> createAudienceOption(
            @Valid @RequestBody CreateSportDictionaryOptionRequest request) {
        return ApiResponse.success("created", adminSportCourseService.createAudienceOption(request.name()));
    }

    /**
     * 新增“器械”字典选项。
     */
    @PostMapping("/options/equipments")
    public ApiResponse<SportDictionaryOptionVO> createEquipmentOption(
            @Valid @RequestBody CreateSportDictionaryOptionRequest request) {
        return ApiResponse.success("created", adminSportCourseService.createEquipmentOption(request.name()));
    }

    /**
     * 新增“收益/效果”字典选项。
     */
    @PostMapping("/options/benefits")
    public ApiResponse<SportDictionaryOptionVO> createBenefitOption(
            @Valid @RequestBody CreateSportDictionaryOptionRequest request) {
        return ApiResponse.success("created", adminSportCourseService.createBenefitOption(request.name()));
    }

    /**
     * 上传课程封面图。
     */
    @PostMapping(value = "/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SportCourseCoverUploadVO> uploadCover(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success("uploaded", adminSportCourseService.uploadCover(file));
    }

    /**
     * 创建课程。
     */
    @PostMapping
    public ApiResponse<SportCourseAdminVO> createCourse(@Valid @RequestBody AdminSaveSportCourseRequest request) {
        return ApiResponse.success("created", adminSportCourseService.createCourse(request));
    }

    /**
     * 更新课程。
     */
    @PutMapping("/{courseId}")
    public ApiResponse<SportCourseAdminVO> updateCourse(
            @PathVariable("courseId")
            @Positive(message = "courseId must be positive")
            Long courseId,
            @Valid @RequestBody AdminSaveSportCourseRequest request) {
        return ApiResponse.success("updated", adminSportCourseService.updateCourse(courseId, request));
    }

    /**
     * 删除课程。
     */
    @DeleteMapping("/{courseId}")
    public ApiResponse<Void> deleteCourse(
            @PathVariable("courseId")
            @Positive(message = "courseId must be positive")
            Long courseId) {
        adminSportCourseService.deleteCourse(courseId);
        return ApiResponse.success("deleted", null);
    }
}
