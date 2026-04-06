package org.hylee.phms.server.service;

import org.hylee.phms.server.dto.CreateMealRecommendationRequest;
import org.hylee.phms.server.dto.RateMealRecommendationRequest;
import org.hylee.phms.server.dto.UpdateMealRecommendationRequest;
import org.hylee.phms.server.vo.MealRecommendationImageUploadVO;
import org.hylee.phms.server.vo.MealRecommendationRatingVO;
import org.hylee.phms.server.vo.MealRecommendationVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MealRecommendationService {

    List<MealRecommendationVO> listRecommendations(String keyword, Integer mealType, Integer limit);

    MealRecommendationVO createRecommendation(CreateMealRecommendationRequest request);

    MealRecommendationVO updateRecommendation(Long recipeId, UpdateMealRecommendationRequest request);

    void deleteRecommendation(Long recipeId);

    MealRecommendationImageUploadVO uploadImage(MultipartFile file);

    MealRecommendationRatingVO rateRecommendation(Long recipeId, RateMealRecommendationRequest request);
}
