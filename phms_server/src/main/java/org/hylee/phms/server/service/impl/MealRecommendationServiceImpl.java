package org.hylee.phms.server.service.impl;

import org.hylee.phms.server.context.LoginUser;
import org.hylee.phms.server.context.LoginUserHolder;
import org.hylee.phms.server.dto.CreateMealRecommendationRequest;
import org.hylee.phms.server.dto.RateMealRecommendationRequest;
import org.hylee.phms.server.dto.UpdateMealRecommendationRequest;
import org.hylee.phms.server.exception.BizException;
import org.hylee.phms.server.mapper.MealRecommendationMapper;
import org.hylee.phms.server.mapper.MealRecommendationRatingMapper;
import org.hylee.phms.server.persistence.MealRecommendationDO;
import org.hylee.phms.server.persistence.MealRecommendationRatingDO;
import org.hylee.phms.server.service.MealRecommendationService;
import org.hylee.phms.server.vo.MealRecommendationImageUploadVO;
import org.hylee.phms.server.vo.MealRecommendationRatingVO;
import org.hylee.phms.server.vo.MealRecommendationVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class MealRecommendationServiceImpl implements MealRecommendationService {

    private static final int DEFAULT_LIMIT = 60;
    private static final int MAX_LIMIT = 200;
    private static final int DEFAULT_MEAL_TYPE = 0;
    private static final int ACTIVE_STATUS = 0;
    private static final String DEFAULT_UNIT = "g";
    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp", "gif");
    private static final String IMAGE_UPLOAD_WEB_PATH = "/static/uploads/recipes/";

    private final MealRecommendationMapper mealRecommendationMapper;
    private final MealRecommendationRatingMapper mealRecommendationRatingMapper;

    public MealRecommendationServiceImpl(MealRecommendationMapper mealRecommendationMapper,
                                         MealRecommendationRatingMapper mealRecommendationRatingMapper) {
        this.mealRecommendationMapper = mealRecommendationMapper;
        this.mealRecommendationRatingMapper = mealRecommendationRatingMapper;
    }

    @Override
    public List<MealRecommendationVO> listRecommendations(String keyword, Integer mealType, Integer limit) {
        LoginUser loginUser = currentLoginUser();
        String normalizedKeyword = normalizeText(keyword);
        Integer normalizedMealType = mealType == null ? null : resolveMealType(mealType);
        int queryLimit = normalizeLimit(limit);
        return mealRecommendationMapper
                .selectRecommendations(loginUser.userId(), loginUser.isAdmin(), normalizedKeyword, normalizedMealType, queryLimit)
                .stream()
                .map(item -> toVO(item, loginUser))
                .toList();
    }

    @Override
    public MealRecommendationVO createRecommendation(CreateMealRecommendationRequest request) {
        LoginUser loginUser = currentLoginUser();
        MealRecommendationDO recommendationDO = new MealRecommendationDO();
        recommendationDO.setCreatedBy(loginUser.userId());
        recommendationDO.setAdminRecommend(loginUser.isAdmin() ? 1 : 0);
        recommendationDO.setRecipeStatus(ACTIVE_STATUS);
        recommendationDO.setMealType(resolveMealType(request.mealType()));
        recommendationDO.setFoodName(requireTrimmed(request.foodName(), "foodName is required"));
        recommendationDO.setPortion(scaleDecimal(request.portion()));
        recommendationDO.setUnit(resolveUnit(request.unit()));
        recommendationDO.setCalories(request.calories());
        recommendationDO.setImageUrl(normalizeText(request.imageUrl()));
        recommendationDO.setDescription(normalizeText(request.description()));
        mealRecommendationMapper.insert(recommendationDO);
        MealRecommendationDO created = mealRecommendationMapper.selectById(recommendationDO.getRecipeId(), loginUser.userId());
        if (created == null) {
            throw recipeNotFound(recommendationDO.getRecipeId());
        }
        return toVO(created, loginUser);
    }

    @Override
    public MealRecommendationVO updateRecommendation(Long recipeId, UpdateMealRecommendationRequest request) {
        LoginUser loginUser = currentLoginUser();
        MealRecommendationDO existing = mealRecommendationMapper.selectById(recipeId, loginUser.userId());
        if (existing == null) {
            throw recipeNotFound(recipeId);
        }
        ensureEditable(existing, loginUser);

        MealRecommendationDO recommendationDO = new MealRecommendationDO();
        recommendationDO.setRecipeId(recipeId);
        recommendationDO.setMealType(resolveMealType(request.mealType()));
        recommendationDO.setFoodName(requireTrimmed(request.foodName(), "foodName is required"));
        recommendationDO.setPortion(scaleDecimal(request.portion()));
        recommendationDO.setUnit(resolveUnit(request.unit()));
        recommendationDO.setCalories(request.calories());
        recommendationDO.setImageUrl(normalizeText(request.imageUrl()));
        recommendationDO.setDescription(normalizeText(request.description()));
        int affected = mealRecommendationMapper.updateById(recommendationDO);
        if (affected == 0) {
            throw recipeNotFound(recipeId);
        }
        MealRecommendationDO updated = mealRecommendationMapper.selectById(recipeId, loginUser.userId());
        if (updated == null) {
            throw recipeNotFound(recipeId);
        }
        return toVO(updated, loginUser);
    }

    @Override
    public void deleteRecommendation(Long recipeId) {
        LoginUser loginUser = currentLoginUser();
        MealRecommendationDO existing = mealRecommendationMapper.selectById(recipeId, loginUser.userId());
        if (existing == null) {
            throw recipeNotFound(recipeId);
        }
        ensureEditable(existing, loginUser);
        int affected = mealRecommendationMapper.softDeleteById(recipeId);
        if (affected == 0) {
            throw recipeNotFound(recipeId);
        }
    }

    @Override
    public MealRecommendationImageUploadVO uploadImage(MultipartFile file) {
        currentLoginUser();
        String extension = validateAndResolveImageExtension(file);
        Path uploadDir = resolveStaticRoot().resolve("uploads").resolve("recipes").toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadDir);
            String storedFilename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
            Path targetFile = uploadDir.resolve(storedFilename).normalize();
            if (!targetFile.startsWith(uploadDir)) {
                throw new BizException(5003, "invalid recipe image upload path");
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return new MealRecommendationImageUploadVO(IMAGE_UPLOAD_WEB_PATH + storedFilename);
        } catch (IOException ex) {
            throw new BizException(5003, "failed to store recipe image: " + ex.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MealRecommendationRatingVO rateRecommendation(Long recipeId, RateMealRecommendationRequest request) {
        LoginUser loginUser = currentLoginUser();
        MealRecommendationDO recipe = mealRecommendationMapper.selectById(recipeId, loginUser.userId());
        if (recipe == null) {
            throw recipeNotFound(recipeId);
        }
        ensureRateable(recipe, loginUser);

        mealRecommendationRatingMapper.upsertRatingLog(recipeId, loginUser.userId(), request.score());
        mealRecommendationRatingMapper.refreshRecipeRating(recipeId);

        MealRecommendationRatingDO ratingDO = mealRecommendationRatingMapper.selectByRecipeAndUser(recipeId, loginUser.userId());
        if (ratingDO == null) {
            return new MealRecommendationRatingVO(recipeId, BigDecimal.ZERO, 0, request.score());
        }
        return new MealRecommendationRatingVO(
                ratingDO.getRecipeId(),
                defaultDecimal(ratingDO.getRatingAvg()),
                defaultInt(ratingDO.getRatingCount()),
                ratingDO.getUserScore()
        );
    }

    private MealRecommendationVO toVO(MealRecommendationDO recommendationDO, LoginUser loginUser) {
        boolean adminRecommend = recommendationDO.getAdminRecommend() != null && recommendationDO.getAdminRecommend() == 1;
        boolean createdByCurrentUser = Objects.equals(recommendationDO.getCreatedBy(), loginUser.userId());
        boolean editable = loginUser.isAdmin() || createdByCurrentUser;
        boolean deletable = editable;
        return new MealRecommendationVO(
                recommendationDO.getRecipeId(),
                recommendationDO.getCreatedBy(),
                recommendationDO.getCreatorAccount(),
                resolveCreatorName(recommendationDO),
                adminRecommend,
                createdByCurrentUser,
                editable,
                deletable,
                recommendationDO.getMealType(),
                recommendationDO.getFoodName(),
                recommendationDO.getPortion(),
                recommendationDO.getUnit(),
                recommendationDO.getCalories(),
                recommendationDO.getImageUrl(),
                recommendationDO.getDescription(),
                defaultDecimal(recommendationDO.getRatingAvg()),
                defaultInt(recommendationDO.getRatingCount()),
                recommendationDO.getUserScore(),
                recommendationDO.getCreatedTime(),
                recommendationDO.getLastChangeTime()
        );
    }

    private String resolveCreatorName(MealRecommendationDO recommendationDO) {
        String realname = normalizeText(recommendationDO.getCreatorRealname());
        if (realname != null) {
            return realname;
        }
        String nickname = normalizeText(recommendationDO.getCreatorNickname());
        if (nickname != null) {
            return nickname;
        }
        return recommendationDO.getCreatorAccount();
    }

    private LoginUser currentLoginUser() {
        LoginUser loginUser = LoginUserHolder.get();
        if (loginUser == null || loginUser.userId() == null) {
            throw new BizException(4010, "please login first", HttpStatus.UNAUTHORIZED);
        }
        return loginUser;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new BizException(4051, "limit must be between 1 and " + MAX_LIMIT);
        }
        return limit;
    }

    private Integer resolveMealType(Integer mealType) {
        if (mealType == null) {
            return DEFAULT_MEAL_TYPE;
        }
        if (mealType < 0 || mealType > 4) {
            throw new BizException(4052, "mealType must be between 0 and 4");
        }
        return mealType;
    }

    private String resolveUnit(String unit) {
        String normalized = normalizeText(unit);
        if (normalized == null) {
            return DEFAULT_UNIT;
        }
        return normalized;
    }

    private void ensureEditable(MealRecommendationDO recommendationDO, LoginUser loginUser) {
        if (!loginUser.isAdmin() && !Objects.equals(recommendationDO.getCreatedBy(), loginUser.userId())) {
            throw new BizException(4055, "no permission to modify this recipe", HttpStatus.FORBIDDEN);
        }
    }

    private void ensureRateable(MealRecommendationDO recommendationDO, LoginUser loginUser) {
        if (loginUser.isAdmin()) {
            return;
        }
        boolean visibleToUser = recommendationDO.getAdminRecommend() != null && recommendationDO.getAdminRecommend() == 1;
        if (visibleToUser || Objects.equals(recommendationDO.getCreatedBy(), loginUser.userId())) {
            return;
        }
        throw new BizException(4056, "no permission to rate this recipe", HttpStatus.FORBIDDEN);
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
            throw new BizException(4053, message);
        }
        return normalized;
    }

    private BigDecimal scaleDecimal(BigDecimal decimal) {
        if (decimal == null) {
            return null;
        }
        return decimal.setScale(2, RoundingMode.HALF_UP);
    }

    private String validateAndResolveImageExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(4005, "recipe image is required");
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new BizException(4005, "recipe image must be <= 5MB", HttpStatus.PAYLOAD_TOO_LARGE);
        }

        String contentType = normalizeText(file.getContentType());
        if (StringUtils.hasText(contentType) && !contentType.startsWith("image/")) {
            throw new BizException(4005, "recipe image must be an image file");
        }

        String extension = extractFileExtension(file.getOriginalFilename());
        if (StringUtils.hasText(extension) && ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            return extension;
        }

        String extensionByContentType = extensionByContentType(contentType);
        if (StringUtils.hasText(extensionByContentType)) {
            return extensionByContentType;
        }
        throw new BizException(4005, "recipe image format must be png/jpg/jpeg/webp/gif");
    }

    private Path resolveStaticRoot() {
        List<Path> candidates = List.of(Paths.get("static"), Paths.get("..", "static"));
        for (Path candidate : candidates) {
            Path absolute = candidate.toAbsolutePath().normalize();
            if (Files.exists(absolute) && Files.isDirectory(absolute)) {
                return absolute;
            }
        }

        Path fallback = Paths.get("static").toAbsolutePath().normalize();
        try {
            Files.createDirectories(fallback);
            return fallback;
        } catch (IOException ex) {
            throw new BizException(5003, "failed to prepare static directory: " + ex.getMessage());
        }
    }

    private String extractFileExtension(String originalFilename) {
        String normalized = normalizeText(originalFilename);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        int lastDot = normalized.lastIndexOf('.');
        if (lastDot < 0 || lastDot == normalized.length() - 1) {
            return null;
        }
        return normalized.substring(lastDot + 1).toLowerCase(Locale.ROOT);
    }

    private String extensionByContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return null;
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> "png";
            case "image/jpg", "image/jpeg", "image/pjpeg" -> "jpg";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            default -> null;
        };
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Integer defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private BizException recipeNotFound(Long recipeId) {
        return new BizException(4054, "recipe not found: " + recipeId, HttpStatus.NOT_FOUND);
    }
}
