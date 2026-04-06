package org.hylee.phms.server.service.impl;

import org.hylee.phms.pojo.model.MealRecord;
import org.hylee.phms.server.context.LoginUser;
import org.hylee.phms.server.context.LoginUserHolder;
import org.hylee.phms.server.dto.CreateMealRecordRequest;
import org.hylee.phms.server.dto.UpdateMealRecordRequest;
import org.hylee.phms.server.exception.BizException;
import org.hylee.phms.server.mapper.MealRecommendationMapper;
import org.hylee.phms.server.mapper.MealRecordMapper;
import org.hylee.phms.server.persistence.MealRecommendationDO;
import org.hylee.phms.server.persistence.MealRecordDO;
import org.hylee.phms.server.service.MealRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class MealRecordServiceImpl implements MealRecordService {

    private static final int DEFAULT_MEAL_TYPE = 0;
    private static final int ACTIVE_STATUS = 0;
    private static final int DELETED_STATUS = 1;
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;
    private static final BigDecimal CALORIE_BASE = BigDecimal.valueOf(100L);

    private final MealRecordMapper mealRecordMapper;
    private final MealRecommendationMapper mealRecommendationMapper;

    public MealRecordServiceImpl(MealRecordMapper mealRecordMapper,
                                 MealRecommendationMapper mealRecommendationMapper) {
        this.mealRecordMapper = mealRecordMapper;
        this.mealRecommendationMapper = mealRecommendationMapper;
    }

    @Override
    @Transactional
    public MealRecord createRecord(CreateMealRecordRequest request) {
        LoginUser loginUser = currentLoginUser();
        RecipeSnapshot snapshot = resolveRecipeSnapshotForCreate(request.recipeId(), loginUser);
        MealRecordDO recordDO = toCreateDO(loginUser.userId(), request, snapshot);
        mealRecordMapper.insert(recordDO);
        return getRecordInternal(recordDO.getRecordId(), loginUser.userId());
    }

    @Override
    @Transactional
    public MealRecord updateRecord(Long recordId, UpdateMealRecordRequest request) {
        LoginUser loginUser = currentLoginUser();
        MealRecordDO existing = mealRecordMapper.selectByIdAndUser(recordId, loginUser.userId());
        if (existing == null) {
            throw mealRecordNotFound(recordId);
        }

        RecipeSnapshot snapshot = resolveRecipeSnapshotForUpdate(existing, request.recipeId(), loginUser);
        MealRecordDO recordDO = toUpdateDO(recordId, loginUser.userId(), request, snapshot);
        int affected = mealRecordMapper.updateByIdAndUser(recordDO);
        if (affected == 0) {
            throw mealRecordNotFound(recordId);
        }
        return getRecordInternal(recordId, loginUser.userId());
    }

    @Override
    @Transactional
    public void deleteRecord(Long recordId) {
        Long userId = currentUserId();
        MealRecordDO existing = mealRecordMapper.selectByIdAndUser(recordId, userId);
        if (existing == null) {
            throw mealRecordNotFound(recordId);
        }
        int affected = mealRecordMapper.softDeleteByIdAndUser(recordId, userId);
        if (affected == 0) {
            throw mealRecordNotFound(recordId);
        }
    }

    @Override
    public MealRecord getRecord(Long recordId) {
        Long userId = currentUserId();
        return getRecordInternal(recordId, userId);
    }

    @Override
    public List<MealRecord> listRecords(LocalDateTime startTime, LocalDateTime endTime, Integer limit) {
        validateTimeRange(startTime, endTime);
        Long userId = currentUserId();
        int queryLimit = normalizeLimit(limit);
        return mealRecordMapper.selectByUserAndRange(userId, startTime, endTime, queryLimit)
                .stream()
                .map(this::toModel)
                .toList();
    }

    private MealRecord getRecordInternal(Long recordId, Long userId) {
        MealRecordDO recordDO = mealRecordMapper.selectByIdAndUser(recordId, userId);
        if (recordDO == null) {
            throw mealRecordNotFound(recordId);
        }
        return toModel(recordDO);
    }

    private MealRecordDO toCreateDO(Long userId, CreateMealRecordRequest request, RecipeSnapshot snapshot) {
        MealRecordDO recordDO = new MealRecordDO();
        recordDO.setUserId(userId);
        recordDO.setRecipeId(snapshot.recipeId());
        recordDO.setFoodName(snapshot.foodName());
        recordDO.setMealType(resolveMealType(request.mealType()));
        recordDO.setDiningTime(request.diningTime());
        recordDO.setIntakeAmount(scaleDecimal(request.intakeAmount()));
        recordDO.setCaloriesPer100g(snapshot.caloriesPer100g());
        recordDO.setEstimatedCalories(calculateEstimatedCalories(recordDO.getIntakeAmount(), snapshot.caloriesPer100g()));
        recordDO.setRemark(normalizeText(request.remark()));
        recordDO.setRecordStatus(ACTIVE_STATUS);
        return recordDO;
    }

    private MealRecordDO toUpdateDO(Long recordId,
                                    Long userId,
                                    UpdateMealRecordRequest request,
                                    RecipeSnapshot snapshot) {
        MealRecordDO recordDO = new MealRecordDO();
        recordDO.setRecordId(recordId);
        recordDO.setUserId(userId);
        recordDO.setRecipeId(snapshot.recipeId());
        recordDO.setFoodName(snapshot.foodName());
        recordDO.setMealType(resolveMealType(request.mealType()));
        recordDO.setDiningTime(request.diningTime());
        recordDO.setIntakeAmount(scaleDecimal(request.intakeAmount()));
        recordDO.setCaloriesPer100g(snapshot.caloriesPer100g());
        recordDO.setEstimatedCalories(calculateEstimatedCalories(recordDO.getIntakeAmount(), snapshot.caloriesPer100g()));
        recordDO.setRemark(normalizeText(request.remark()));
        return recordDO;
    }

    private RecipeSnapshot resolveRecipeSnapshotForCreate(Long recipeId, LoginUser loginUser) {
        MealRecommendationDO recipe = loadAccessibleRecipe(recipeId, loginUser);
        return snapshotFromRecipe(recipe);
    }

    private RecipeSnapshot resolveRecipeSnapshotForUpdate(MealRecordDO existing, Long recipeId, LoginUser loginUser) {
        if (Objects.equals(existing.getRecipeId(), recipeId)) {
            return snapshotFromExisting(existing);
        }
        MealRecommendationDO recipe = loadAccessibleRecipe(recipeId, loginUser);
        return snapshotFromRecipe(recipe);
    }

    private RecipeSnapshot snapshotFromRecipe(MealRecommendationDO recipe) {
        if (recipe == null) {
            throw recipeNotFound(null);
        }
        String foodName = requireTrimmed(recipe.getFoodName(), "recipe foodName is required");
        Integer calories = recipe.getCalories();
        if (calories == null) {
            throw recipeCaloriesMissing(recipe.getRecipeId());
        }
        BigDecimal caloriesPer100g = BigDecimal.valueOf(calories.longValue()).setScale(2, RoundingMode.HALF_UP);
        return new RecipeSnapshot(recipe.getRecipeId(), foodName, caloriesPer100g);
    }

    private RecipeSnapshot snapshotFromExisting(MealRecordDO existing) {
        String foodName = requireTrimmed(existing.getFoodName(), "record foodName is required");
        BigDecimal caloriesPer100g = scaleDecimal(existing.getCaloriesPer100g());
        if (caloriesPer100g == null) {
            throw recipeCaloriesMissing(existing.getRecipeId());
        }
        return new RecipeSnapshot(existing.getRecipeId(), foodName, caloriesPer100g);
    }

    private MealRecommendationDO loadAccessibleRecipe(Long recipeId, LoginUser loginUser) {
        MealRecommendationDO recipe = mealRecommendationMapper.selectById(recipeId, loginUser.userId());
        if (recipe == null || recipe.getRecipeStatus() != null && recipe.getRecipeStatus() == DELETED_STATUS) {
            throw recipeNotFound(recipeId);
        }
        if (loginUser.isAdmin()) {
            return recipe;
        }
        boolean adminRecommend = recipe.getAdminRecommend() != null && recipe.getAdminRecommend() == 1;
        boolean ownedByCurrentUser = Objects.equals(recipe.getCreatedBy(), loginUser.userId());
        if (adminRecommend || ownedByCurrentUser) {
            return recipe;
        }
        throw new BizException(4018, "no permission to use this recipe", HttpStatus.FORBIDDEN);
    }

    private MealRecord toModel(MealRecordDO recordDO) {
        return new MealRecord(
                recordDO.getRecordId(),
                recordDO.getRecipeId(),
                recordDO.getFoodName(),
                recordDO.getMealType(),
                recordDO.getDiningTime(),
                recordDO.getIntakeAmount(),
                recordDO.getCaloriesPer100g(),
                recordDO.getEstimatedCalories(),
                recordDO.getRemark(),
                recordDO.getCreatedTime(),
                recordDO.getLastChangeTime()
        );
    }

    private Integer resolveMealType(Integer mealType) {
        if (mealType == null) {
            return DEFAULT_MEAL_TYPE;
        }
        if (mealType < 0 || mealType > 4) {
            throw new BizException(4013, "mealType must be between 0 and 4");
        }
        return mealType;
    }

    private BigDecimal calculateEstimatedCalories(BigDecimal intakeAmount, BigDecimal caloriesPer100g) {
        return intakeAmount.multiply(caloriesPer100g)
                .divide(CALORIE_BASE, 2, RoundingMode.HALF_UP);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new BizException(4008, "limit must be between 1 and " + MAX_LIMIT);
        }
        return limit;
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new BizException(4007, "endTime must be greater than or equal to startTime");
        }
    }

    private LoginUser currentLoginUser() {
        LoginUser loginUser = LoginUserHolder.get();
        if (loginUser == null || loginUser.userId() == null) {
            throw new BizException(4010, "please login first", HttpStatus.UNAUTHORIZED);
        }
        return loginUser;
    }

    private Long currentUserId() {
        return currentLoginUser().userId();
    }

    private String normalizeText(String value) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return null;
    }

    private String requireTrimmed(String value, String message) {
        String normalized = normalizeText(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(4009, message);
        }
        return normalized;
    }

    private BigDecimal scaleDecimal(BigDecimal decimal) {
        if (decimal == null) {
            return null;
        }
        return decimal.setScale(2, RoundingMode.HALF_UP);
    }

    private BizException mealRecordNotFound(Long recordId) {
        return new BizException(4043, "meal record not found: " + recordId, HttpStatus.NOT_FOUND);
    }

    private BizException recipeNotFound(Long recipeId) {
        return new BizException(4054, "recipe not found: " + recipeId, HttpStatus.NOT_FOUND);
    }

    private BizException recipeCaloriesMissing(Long recipeId) {
        return new BizException(4019, "recipe calories per 100g is required: " + recipeId, HttpStatus.BAD_REQUEST);
    }

    private record RecipeSnapshot(Long recipeId, String foodName, BigDecimal caloriesPer100g) {
    }
}
