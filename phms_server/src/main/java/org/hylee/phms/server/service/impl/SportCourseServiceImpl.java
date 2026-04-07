package org.hylee.phms.server.service.impl;

import org.hylee.phms.server.context.LoginUserHolder;
import org.hylee.phms.server.dto.RateSportCourseRequest;
import org.hylee.phms.server.exception.BizException;
import org.hylee.phms.server.mapper.SportCourseMapper;
import org.hylee.phms.server.mapper.SportCourseRatingMapper;
import org.hylee.phms.server.persistence.SportCourseDO;
import org.hylee.phms.server.persistence.SportCourseRatingDO;
import org.hylee.phms.server.service.SportCourseService;
import org.hylee.phms.server.vo.SportCourseCardVO;
import org.hylee.phms.server.vo.SportCourseRatingVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * {@link org.hylee.phms.server.service.SportCourseService} 实现。
 */
@Service
public class SportCourseServiceImpl implements SportCourseService {

    private static final int DEFAULT_LIMIT = 30;
    private static final int MAX_LIMIT = 200;

    private final SportCourseMapper sportCourseMapper;
    private final SportCourseRatingMapper sportCourseRatingMapper;

    public SportCourseServiceImpl(SportCourseMapper sportCourseMapper,
                                  SportCourseRatingMapper sportCourseRatingMapper) {
        this.sportCourseMapper = sportCourseMapper;
        this.sportCourseRatingMapper = sportCourseRatingMapper;
    }

    @Override
    public List<SportCourseCardVO> listPublishedCourses(String keyword, Integer limit) {
        Long userId = currentUserId();
        Integer queryLimit = normalizeLimit(limit);
        String normalizedKeyword = normalizeText(keyword);
        return sportCourseMapper.selectPublishedCourses(normalizedKeyword, queryLimit, userId).stream()
                .map(this::toCardVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SportCourseRatingVO rateCourse(Long courseId, RateSportCourseRequest request) {
        Long userId = currentUserId();
        SportCourseDO courseDO = sportCourseMapper.selectBaseById(courseId);
        if (courseDO == null || isDeleted(courseDO)) {
            throw new BizException(4044, "sport course not found: " + courseId, HttpStatus.NOT_FOUND);
        }
        if (!"published".equals(courseDO.getStatus())) {
            throw new BizException(4009, "only published courses can be rated");
        }

        String comment = normalizeText(request.comment());
        sportCourseRatingMapper.upsertRatingLog(courseId, userId, request.score(), comment);
        sportCourseRatingMapper.refreshCourseRating(courseId);

        SportCourseRatingDO ratingDO = sportCourseRatingMapper.selectByCourseAndUser(courseId, userId);
        if (ratingDO == null) {
            return new SportCourseRatingVO(courseId, BigDecimal.ZERO, 0, request.score());
        }
        return new SportCourseRatingVO(
                ratingDO.getCourseId(),
                defaultDecimal(ratingDO.getRatingAvg()),
                defaultInt(ratingDO.getRatingCount()),
                ratingDO.getUserScore()
        );
    }

    private SportCourseCardVO toCardVO(SportCourseDO courseDO) {
        return new SportCourseCardVO(
                courseDO.getId(),
                courseDO.getName(),
                courseDO.getCoverUrl(),
                courseDO.getSummary(),
                courseDO.getDescription(),
                courseDO.getRecommendDurationMin(),
                courseDO.getCaloriesPerHour(),
                courseDO.getRecommendFrequencyPerWeek(),
                courseDO.getLevel(),
                defaultDecimal(courseDO.getRatingAvg()),
                defaultInt(courseDO.getRatingCount()),
                courseDO.getUserScore(),
                splitCsv(courseDO.getAudiencesCsv()),
                splitCsv(courseDO.getEquipmentsCsv()),
                splitCsv(courseDO.getBenefitsCsv()),
                courseDO.getCreatedAt(),
                courseDO.getUpdatedAt()
        );
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

    private Long currentUserId() {
        Long userId = LoginUserHolder.getUserId();
        if (userId == null) {
            throw new BizException(4010, "please login first", HttpStatus.UNAUTHORIZED);
        }
        return userId;
    }

    private Integer normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new BizException(4008, "limit must be between 1 and " + MAX_LIMIT);
        }
        return limit;
    }

    private String normalizeText(String value) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return null;
    }

    private boolean isDeleted(SportCourseDO courseDO) {
        return courseDO.getIsDeleted() != null && courseDO.getIsDeleted() == 1;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Integer defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
