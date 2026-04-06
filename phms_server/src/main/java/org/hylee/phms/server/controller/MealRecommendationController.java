package org.hylee.phms.server.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.hylee.phms.common.api.ApiResponse;
import org.hylee.phms.server.dto.CreateMealRecommendationRequest;
import org.hylee.phms.server.dto.RateMealRecommendationRequest;
import org.hylee.phms.server.dto.UpdateMealRecommendationRequest;
import org.hylee.phms.server.service.MealRecommendationService;
import org.hylee.phms.server.vo.MealRecommendationImageUploadVO;
import org.hylee.phms.server.vo.MealRecommendationRatingVO;
import org.hylee.phms.server.vo.MealRecommendationVO;
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
@RequestMapping("/api/health/meal-recommendations")
public class MealRecommendationController {

    private final MealRecommendationService mealRecommendationService;

    public MealRecommendationController(MealRecommendationService mealRecommendationService) {
        this.mealRecommendationService = mealRecommendationService;
    }

    @GetMapping
    public ApiResponse<List<MealRecommendationVO>> listRecommendations(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "mealType", required = false)
            @Min(value = 0, message = "mealType must be between 0 and 4")
            @Max(value = 4, message = "mealType must be between 0 and 4")
            Integer mealType,
            @RequestParam(name = "limit", defaultValue = "60")
            @Min(value = 1, message = "limit must be >= 1")
            @Max(value = 200, message = "limit must be <= 200")
            Integer limit) {
        return ApiResponse.success(mealRecommendationService.listRecommendations(keyword, mealType, limit));
    }

    @PostMapping
    public ApiResponse<MealRecommendationVO> createRecommendation(
            @Valid @RequestBody CreateMealRecommendationRequest request) {
        return ApiResponse.success("created", mealRecommendationService.createRecommendation(request));
    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MealRecommendationImageUploadVO> uploadImage(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success("uploaded", mealRecommendationService.uploadImage(file));
    }

    @PutMapping("/{recipeId:\\d+}")
    public ApiResponse<MealRecommendationVO> updateRecommendation(
            @PathVariable("recipeId")
            @Positive(message = "recipeId must be positive")
            Long recipeId,
            @Valid @RequestBody UpdateMealRecommendationRequest request) {
        return ApiResponse.success("updated", mealRecommendationService.updateRecommendation(recipeId, request));
    }

    @PostMapping("/{recipeId:\\d+}/ratings")
    public ApiResponse<MealRecommendationRatingVO> rateRecommendation(
            @PathVariable("recipeId")
            @Positive(message = "recipeId must be positive")
            Long recipeId,
            @Valid @RequestBody RateMealRecommendationRequest request) {
        return ApiResponse.success("rated", mealRecommendationService.rateRecommendation(recipeId, request));
    }

    @DeleteMapping("/{recipeId:\\d+}")
    public ApiResponse<Void> deleteRecommendation(
            @PathVariable("recipeId")
            @Positive(message = "recipeId must be positive")
            Long recipeId) {
        mealRecommendationService.deleteRecommendation(recipeId);
        return ApiResponse.success("deleted", null);
    }
}
