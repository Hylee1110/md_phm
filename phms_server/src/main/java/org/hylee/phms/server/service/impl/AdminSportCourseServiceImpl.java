package org.hylee.phms.server.service.impl;

import org.hylee.phms.server.dto.AdminSaveSportCourseRequest;
import org.hylee.phms.server.exception.BizException;
import org.hylee.phms.server.mapper.SportCourseMapper;
import org.hylee.phms.server.mapper.SportDictionaryMapper;
import org.hylee.phms.server.persistence.SportCourseDO;
import org.hylee.phms.server.persistence.SportDictionaryOptionDO;
import org.hylee.phms.server.service.AdminSportCourseService;
import org.hylee.phms.server.vo.SportCourseAdminVO;
import org.hylee.phms.server.vo.SportCourseCoverUploadVO;
import org.hylee.phms.server.vo.SportCourseOptionsVO;
import org.hylee.phms.server.vo.SportDictionaryOptionVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * {@link org.hylee.phms.server.service.AdminSportCourseService} 实现。
 * <p>
 * 课程 CRUD、字典维护、封面上传至本地静态目录等。
 */
@Service
public class AdminSportCourseServiceImpl implements AdminSportCourseService {

    private static final Set<String> ALLOWED_LEVELS = Set.of("beginner", "intermediate", "advanced", "all");
    private static final Set<String> ALLOWED_STATUS = Set.of("draft", "published", "archived");
    private static final long MAX_COVER_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_COVER_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp", "gif");
    private static final String COVER_UPLOAD_WEB_PATH = "/static/uploads/sport-courses/";

    private final SportCourseMapper sportCourseMapper;
    private final SportDictionaryMapper sportDictionaryMapper;

    public AdminSportCourseServiceImpl(SportCourseMapper sportCourseMapper,
                                       SportDictionaryMapper sportDictionaryMapper) {
        this.sportCourseMapper = sportCourseMapper;
        this.sportDictionaryMapper = sportDictionaryMapper;
    }

    @Override
    public List<SportCourseAdminVO> listCourses(String keyword, String status) {
        String normalizedKeyword = normalizeText(keyword);
        String normalizedStatus = normalizeStatus(status, true);
        return sportCourseMapper.selectAdminCourses(normalizedKeyword, normalizedStatus).stream()
                .map(courseDO -> toAdminVO(courseDO, List.of(), List.of(), List.of()))
                .toList();
    }

    @Override
    public SportCourseAdminVO getCourse(Long courseId) {
        SportCourseDO courseDO = sportCourseMapper.selectAdminCourseById(courseId);
        if (courseDO == null) {
            throw new BizException(4044, "sport course not found: " + courseId, HttpStatus.NOT_FOUND);
        }
        List<Long> audienceIds = sportCourseMapper.selectAudienceIds(courseId);
        List<Long> equipmentIds = sportCourseMapper.selectEquipmentIds(courseId);
        List<Long> benefitIds = sportCourseMapper.selectBenefitIds(courseId);
        return toAdminVO(courseDO, audienceIds, equipmentIds, benefitIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SportCourseAdminVO createCourse(AdminSaveSportCourseRequest request) {
        NormalizedRequest normalized = normalizeAndValidate(request, null);

        SportCourseDO courseDO = new SportCourseDO();
        fillCourseDO(courseDO, normalized);
        sportCourseMapper.insertCourse(courseDO);

        applyRelations(courseDO.getId(), normalized);
        return getCourse(courseDO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SportCourseAdminVO updateCourse(Long courseId, AdminSaveSportCourseRequest request) {
        SportCourseDO existing = sportCourseMapper.selectAdminCourseById(courseId);
        if (existing == null) {
            throw new BizException(4044, "sport course not found: " + courseId, HttpStatus.NOT_FOUND);
        }

        NormalizedRequest normalized = normalizeAndValidate(request, courseId);
        SportCourseDO courseDO = new SportCourseDO();
        courseDO.setId(courseId);
        fillCourseDO(courseDO, normalized);
        int affected = sportCourseMapper.updateCourse(courseDO);
        if (affected == 0) {
            throw new BizException(4044, "sport course not found: " + courseId, HttpStatus.NOT_FOUND);
        }

        applyRelations(courseId, normalized);
        return getCourse(courseId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCourse(Long courseId) {
        int affected = sportCourseMapper.softDeleteCourse(courseId);
        if (affected == 0) {
            throw new BizException(4044, "sport course not found: " + courseId, HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public SportCourseOptionsVO getOptions() {
        List<SportDictionaryOptionVO> audiences = sportDictionaryMapper.selectAudiences().stream()
                .map(this::toOptionVO)
                .toList();
        List<SportDictionaryOptionVO> equipments = sportDictionaryMapper.selectEquipments().stream()
                .map(this::toOptionVO)
                .toList();
        List<SportDictionaryOptionVO> benefits = sportDictionaryMapper.selectBenefits().stream()
                .map(this::toOptionVO)
                .toList();
        return new SportCourseOptionsVO(audiences, equipments, benefits);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SportDictionaryOptionVO createAudienceOption(String name) {
        String normalized = normalizeDictionaryName(name);
        sportDictionaryMapper.insertAudience(normalized);
        SportDictionaryOptionDO optionDO = sportDictionaryMapper.selectAudienceByName(normalized);
        if (optionDO == null) {
            throw new BizException(5002, "failed to create audience option");
        }
        return toOptionVO(optionDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SportDictionaryOptionVO createEquipmentOption(String name) {
        String normalized = normalizeDictionaryName(name);
        sportDictionaryMapper.insertEquipment(normalized);
        SportDictionaryOptionDO optionDO = sportDictionaryMapper.selectEquipmentByName(normalized);
        if (optionDO == null) {
            throw new BizException(5002, "failed to create equipment option");
        }
        return toOptionVO(optionDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SportDictionaryOptionVO createBenefitOption(String name) {
        String normalized = normalizeDictionaryName(name);
        sportDictionaryMapper.insertBenefit(normalized);
        SportDictionaryOptionDO optionDO = sportDictionaryMapper.selectBenefitByName(normalized);
        if (optionDO == null) {
            throw new BizException(5002, "failed to create benefit option");
        }
        return toOptionVO(optionDO);
    }

    @Override
    public SportCourseCoverUploadVO uploadCover(MultipartFile file) {
        String extension = validateAndResolveCoverExtension(file);
        Path uploadDir = resolveStaticRoot().resolve("uploads").resolve("sport-courses").toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadDir);
            String storedFilename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
            Path targetFile = uploadDir.resolve(storedFilename).normalize();
            if (!targetFile.startsWith(uploadDir)) {
                throw new BizException(5003, "invalid cover upload path");
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
            return new SportCourseCoverUploadVO(COVER_UPLOAD_WEB_PATH + storedFilename);
        } catch (IOException ex) {
            throw new BizException(5003, "failed to store cover image: " + ex.getMessage());
        }
    }

    private void fillCourseDO(SportCourseDO courseDO, NormalizedRequest normalized) {
        courseDO.setName(normalized.name());
        courseDO.setCoverUrl(normalized.coverUrl());
        courseDO.setSummary(normalized.summary());
        courseDO.setDescription(normalized.description());
        courseDO.setRecommendDurationMin(normalized.recommendDurationMin());
        courseDO.setCaloriesPerHour(normalized.caloriesPerHour());
        courseDO.setRecommendFrequencyPerWeek(normalized.recommendFrequencyPerWeek());
        courseDO.setLevel(normalized.level());
        courseDO.setStatus(normalized.status());
        courseDO.setSortWeight(normalized.sortWeight());
    }

    private void applyRelations(Long courseId, NormalizedRequest normalized) {
        sportCourseMapper.deleteCourseAudiences(courseId);
        sportCourseMapper.deleteCourseEquipments(courseId);
        sportCourseMapper.deleteCourseBenefits(courseId);

        if (!normalized.audienceIds().isEmpty()) {
            sportCourseMapper.insertCourseAudiences(courseId, normalized.audienceIds());
        }
        if (!normalized.equipmentIds().isEmpty()) {
            sportCourseMapper.insertCourseEquipments(courseId, normalized.equipmentIds());
        }
        if (!normalized.benefitIds().isEmpty()) {
            sportCourseMapper.insertCourseBenefits(courseId, normalized.benefitIds());
        }
    }

    private NormalizedRequest normalizeAndValidate(AdminSaveSportCourseRequest request, Long excludeId) {
        String name = normalizeText(request.name());
        if (!StringUtils.hasText(name)) {
            throw new BizException(4005, "name is required");
        }
        Long duplicate = sportCourseMapper.countByName(name, excludeId);
        if (duplicate != null && duplicate > 0) {
            throw new BizException(4092, "course name already exists: " + name, HttpStatus.CONFLICT);
        }

        String level = normalizeLevel(request.level());
        String status = normalizeStatus(request.status(), false);
        Integer sortWeight = request.sortWeight() == null ? 0 : request.sortWeight();
        List<Long> audienceIds = normalizeIdList(request.audienceIds(), "audienceIds");
        List<Long> equipmentIds = normalizeIdList(request.equipmentIds(), "equipmentIds");
        List<Long> benefitIds = normalizeIdList(request.benefitIds(), "benefitIds");

        validateOptionIds("audienceIds", audienceIds, countAudienceIds(audienceIds));
        validateOptionIds("equipmentIds", equipmentIds, countEquipmentIds(equipmentIds));
        validateOptionIds("benefitIds", benefitIds, countBenefitIds(benefitIds));

        return new NormalizedRequest(
                name,
                normalizeText(request.coverUrl()),
                normalizeText(request.summary()),
                normalizeText(request.description()),
                request.recommendDurationMin(),
                request.caloriesPerHour(),
                request.recommendFrequencyPerWeek(),
                level,
                status,
                sortWeight,
                audienceIds,
                equipmentIds,
                benefitIds
        );
    }

    private void validateOptionIds(String fieldName, List<Long> ids, Long count) {
        if (ids.isEmpty()) {
            return;
        }
        long actualCount = count == null ? 0L : count;
        if (actualCount != ids.size()) {
            throw new BizException(4006, fieldName + " contains invalid id");
        }
    }

    private Long countAudienceIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0L;
        }
        return sportDictionaryMapper.countAudienceIds(ids);
    }

    private Long countEquipmentIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0L;
        }
        return sportDictionaryMapper.countEquipmentIds(ids);
    }

    private Long countBenefitIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return 0L;
        }
        return sportDictionaryMapper.countBenefitIds(ids);
    }

    private List<Long> normalizeIdList(List<Long> rawIds, String fieldName) {
        if (rawIds == null || rawIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> dedup = new LinkedHashSet<>();
        for (Long id : rawIds) {
            if (id == null || id <= 0) {
                throw new BizException(4004, fieldName + " must contain positive id");
            }
            dedup.add(id);
        }
        return List.copyOf(dedup);
    }

    private SportCourseAdminVO toAdminVO(SportCourseDO courseDO,
                                         List<Long> audienceIds,
                                         List<Long> equipmentIds,
                                         List<Long> benefitIds) {
        return new SportCourseAdminVO(
                courseDO.getId(),
                courseDO.getName(),
                courseDO.getCoverUrl(),
                courseDO.getSummary(),
                courseDO.getDescription(),
                courseDO.getRecommendDurationMin(),
                courseDO.getCaloriesPerHour(),
                courseDO.getRecommendFrequencyPerWeek(),
                courseDO.getLevel(),
                courseDO.getStatus(),
                courseDO.getSortWeight(),
                defaultDecimal(courseDO.getRatingAvg()),
                defaultInt(courseDO.getRatingCount()),
                audienceIds,
                equipmentIds,
                benefitIds,
                splitCsv(courseDO.getAudiencesCsv()),
                splitCsv(courseDO.getEquipmentsCsv()),
                splitCsv(courseDO.getBenefitsCsv()),
                courseDO.getCreatedAt(),
                courseDO.getUpdatedAt()
        );
    }

    private SportDictionaryOptionVO toOptionVO(SportDictionaryOptionDO optionDO) {
        return new SportDictionaryOptionVO(optionDO.getId(), optionDO.getName());
    }

    private String validateAndResolveCoverExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(4005, "cover image is required");
        }
        if (file.getSize() > MAX_COVER_SIZE_BYTES) {
            throw new BizException(4005, "cover image must be <= 5MB", HttpStatus.PAYLOAD_TOO_LARGE);
        }

        String contentType = normalizeText(file.getContentType());
        if (StringUtils.hasText(contentType) && !contentType.startsWith("image/")) {
            throw new BizException(4005, "cover image must be an image file");
        }

        String extension = extractFileExtension(file.getOriginalFilename());
        if (StringUtils.hasText(extension) && ALLOWED_COVER_EXTENSIONS.contains(extension)) {
            return extension;
        }

        String extensionByContentType = extensionByContentType(contentType);
        if (StringUtils.hasText(extensionByContentType)) {
            return extensionByContentType;
        }
        throw new BizException(4005, "cover image format must be png/jpg/jpeg/webp/gif");
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

    private String normalizeLevel(String level) {
        String normalized = normalizeText(level);
        if (!ALLOWED_LEVELS.contains(normalized)) {
            throw new BizException(4007, "invalid level: " + level);
        }
        return normalized;
    }

    private String normalizeStatus(String status, boolean nullable) {
        String normalized = normalizeText(status);
        if (!StringUtils.hasText(normalized)) {
            if (nullable) {
                return null;
            }
            throw new BizException(4007, "status is required");
        }
        if (!ALLOWED_STATUS.contains(normalized)) {
            throw new BizException(4007, "invalid status: " + status);
        }
        return normalized;
    }

    private List<String> splitCsv(String csv) {
        if (!StringUtils.hasText(csv)) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private String normalizeText(String value) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return null;
    }

    private String normalizeDictionaryName(String name) {
        String normalized = normalizeText(name);
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(4005, "name is required");
        }
        if (Objects.requireNonNull(normalized).length() > 32) {
            throw new BizException(4005, "name length must be <= 32");
        }
        return normalized;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Integer defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private record NormalizedRequest(
            String name,
            String coverUrl,
            String summary,
            String description,
            Integer recommendDurationMin,
            Integer caloriesPerHour,
            Integer recommendFrequencyPerWeek,
            String level,
            String status,
            Integer sortWeight,
            List<Long> audienceIds,
            List<Long> equipmentIds,
            List<Long> benefitIds
    ) {
    }
}
