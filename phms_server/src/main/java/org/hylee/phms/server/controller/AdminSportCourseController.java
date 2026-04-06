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

@Validated
@RestController
@RequestMapping("/api/admin/sport-courses")
public class AdminSportCourseController {

    private final AdminSportCourseService adminSportCourseService;

    public AdminSportCourseController(AdminSportCourseService adminSportCourseService) {
        this.adminSportCourseService = adminSportCourseService;
    }

    @GetMapping
    public ApiResponse<List<SportCourseAdminVO>> listCourses(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) String status) {
        return ApiResponse.success(adminSportCourseService.listCourses(keyword, status));
    }

    @GetMapping("/options")
    public ApiResponse<SportCourseOptionsVO> options() {
        return ApiResponse.success(adminSportCourseService.getOptions());
    }

    @GetMapping("/{courseId}")
    public ApiResponse<SportCourseAdminVO> getCourse(
            @PathVariable("courseId")
            @Positive(message = "courseId must be positive")
            Long courseId) {
        return ApiResponse.success(adminSportCourseService.getCourse(courseId));
    }

    @PostMapping("/options/audiences")
    public ApiResponse<SportDictionaryOptionVO> createAudienceOption(
            @Valid @RequestBody CreateSportDictionaryOptionRequest request) {
        return ApiResponse.success("created", adminSportCourseService.createAudienceOption(request.name()));
    }

    @PostMapping("/options/equipments")
    public ApiResponse<SportDictionaryOptionVO> createEquipmentOption(
            @Valid @RequestBody CreateSportDictionaryOptionRequest request) {
        return ApiResponse.success("created", adminSportCourseService.createEquipmentOption(request.name()));
    }

    @PostMapping("/options/benefits")
    public ApiResponse<SportDictionaryOptionVO> createBenefitOption(
            @Valid @RequestBody CreateSportDictionaryOptionRequest request) {
        return ApiResponse.success("created", adminSportCourseService.createBenefitOption(request.name()));
    }

    @PostMapping(value = "/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<SportCourseCoverUploadVO> uploadCover(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success("uploaded", adminSportCourseService.uploadCover(file));
    }

    @PostMapping
    public ApiResponse<SportCourseAdminVO> createCourse(@Valid @RequestBody AdminSaveSportCourseRequest request) {
        return ApiResponse.success("created", adminSportCourseService.createCourse(request));
    }

    @PutMapping("/{courseId}")
    public ApiResponse<SportCourseAdminVO> updateCourse(
            @PathVariable("courseId")
            @Positive(message = "courseId must be positive")
            Long courseId,
            @Valid @RequestBody AdminSaveSportCourseRequest request) {
        return ApiResponse.success("updated", adminSportCourseService.updateCourse(courseId, request));
    }

    @DeleteMapping("/{courseId}")
    public ApiResponse<Void> deleteCourse(
            @PathVariable("courseId")
            @Positive(message = "courseId must be positive")
            Long courseId) {
        adminSportCourseService.deleteCourse(courseId);
        return ApiResponse.success("deleted", null);
    }
}
