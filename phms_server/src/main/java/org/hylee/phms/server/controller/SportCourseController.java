package org.hylee.phms.server.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.hylee.phms.common.api.ApiResponse;
import org.hylee.phms.server.dto.RateSportCourseRequest;
import org.hylee.phms.server.service.SportCourseService;
import org.hylee.phms.server.vo.SportCourseCardVO;
import org.hylee.phms.server.vo.SportCourseRatingVO;
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
@RequestMapping("/api/health/sport-courses")
public class SportCourseController {

    private final SportCourseService sportCourseService;

    public SportCourseController(SportCourseService sportCourseService) {
        this.sportCourseService = sportCourseService;
    }

    @GetMapping
    public ApiResponse<List<SportCourseCardVO>> listCourses(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "limit", defaultValue = "30")
            @Min(value = 1, message = "limit must be >= 1")
            @Max(value = 200, message = "limit must be <= 200")
            Integer limit) {
        return ApiResponse.success(sportCourseService.listPublishedCourses(keyword, limit));
    }

    @PostMapping("/{courseId}/ratings")
    public ApiResponse<SportCourseRatingVO> rateCourse(
            @PathVariable("courseId")
            @Positive(message = "courseId must be positive")
            Long courseId,
            @Valid @RequestBody RateSportCourseRequest request) {
        return ApiResponse.success("rated", sportCourseService.rateCourse(courseId, request));
    }
}
