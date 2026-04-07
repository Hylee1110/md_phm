package org.hylee.phms.server.service.impl;

import org.hylee.phms.server.context.LoginUserHolder;
import org.hylee.phms.server.dto.SelectHealthGoalRequest;
import org.hylee.phms.server.exception.BizException;
import org.hylee.phms.server.mapper.HealthGoalMapper;
import org.hylee.phms.server.mapper.UserHealthGoalMapper;
import org.hylee.phms.server.persistence.HealthGoalDO;
import org.hylee.phms.server.persistence.UserHealthGoalDO;
import org.hylee.phms.server.service.HealthGoalService;
import org.hylee.phms.server.vo.HealthGoalCardVO;
import org.hylee.phms.server.vo.UserHealthGoalVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * {@link org.hylee.phms.server.service.HealthGoalService} 实现。
 */
@Service
public class HealthGoalServiceImpl implements HealthGoalService {

    private static final int STATUS_ACTIVE = 0;
    private static final int STATUS_CANCELLED = 2;

    private final HealthGoalMapper healthGoalMapper;
    private final UserHealthGoalMapper userHealthGoalMapper;

    public HealthGoalServiceImpl(HealthGoalMapper healthGoalMapper, UserHealthGoalMapper userHealthGoalMapper) {
        this.healthGoalMapper = healthGoalMapper;
        this.userHealthGoalMapper = userHealthGoalMapper;
    }

    @Override
    public List<HealthGoalCardVO> listAvailableGoals(String keyword) {
        Long userId = currentUserId();
        String normalizedKeyword = normalizeText(keyword);
        return healthGoalMapper.selectAvailableGoals(userId, normalizedKeyword).stream()
                .map(this::toCardVO)
                .toList();
    }

    @Override
    public List<UserHealthGoalVO> listUserGoals(Integer userGoalStatus) {
        Long userId = currentUserId();
        return userHealthGoalMapper.selectByUser(userId, userGoalStatus).stream()
                .map(this::toUserGoalVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserHealthGoalVO selectGoal(Long goalId, SelectHealthGoalRequest request) {
        Long userId = currentUserId();
        HealthGoalDO goalDO = healthGoalMapper.selectById(goalId);
        if (goalDO == null) {
            throw new BizException(4045, "health goal not found: " + goalId, HttpStatus.NOT_FOUND);
        }
        if (goalDO.getGoalStatus() == null || goalDO.getGoalStatus() != STATUS_ACTIVE) {
            throw new BizException(4009, "health goal is disabled");
        }

        UserHealthGoalDO existing = userHealthGoalMapper.selectByUserAndGoal(userId, goalId);
        NormalizedSelection normalized = normalizeSelection(goalDO, existing, request);

        Long userGoalId;
        if (existing == null) {
            UserHealthGoalDO userHealthGoalDO = new UserHealthGoalDO();
            userHealthGoalDO.setUserId(userId);
            userHealthGoalDO.setGoalId(goalId);
            userHealthGoalDO.setTargetMin(normalized.targetMin());
            userHealthGoalDO.setTargetMax(normalized.targetMax());
            userHealthGoalDO.setTargetText(normalized.targetText());
            userHealthGoalDO.setStartDate(normalized.startDate());
            userHealthGoalDO.setEndDate(normalized.endDate());
            userHealthGoalDO.setUserGoalStatus(STATUS_ACTIVE);
            userHealthGoalMapper.insert(userHealthGoalDO);
            userGoalId = userHealthGoalDO.getUserGoalId();
        } else {
            existing.setTargetMin(normalized.targetMin());
            existing.setTargetMax(normalized.targetMax());
            existing.setTargetText(normalized.targetText());
            existing.setStartDate(normalized.startDate());
            existing.setEndDate(normalized.endDate());
            existing.setUserGoalStatus(STATUS_ACTIVE);
            int affected = userHealthGoalMapper.updateById(existing);
            if (affected == 0) {
                throw new BizException(5004, "failed to update user goal", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            userGoalId = existing.getUserGoalId();
        }

        UserHealthGoalDO refreshed = userHealthGoalMapper.selectByIdAndUser(userGoalId, userId);
        if (refreshed == null) {
            throw new BizException(5004, "failed to load selected user goal", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return toUserGoalVO(refreshed);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelGoal(Long userGoalId) {
        Long userId = currentUserId();
        UserHealthGoalDO existing = userHealthGoalMapper.selectByIdAndUser(userGoalId, userId);
        if (existing == null) {
            throw new BizException(4046, "user health goal not found: " + userGoalId, HttpStatus.NOT_FOUND);
        }
        int affected = userHealthGoalMapper.updateStatusByIdAndUser(userGoalId, userId, STATUS_CANCELLED);
        if (affected == 0) {
            throw new BizException(4046, "user health goal not found: " + userGoalId, HttpStatus.NOT_FOUND);
        }
    }

    private NormalizedSelection normalizeSelection(HealthGoalDO goalDO,
                                                   UserHealthGoalDO existing,
                                                   SelectHealthGoalRequest request) {
        BigDecimal targetMin;
        BigDecimal targetMax;
        String targetText;

        if (goalDO.getMetricType() != null && goalDO.getMetricType() == 1) {
            targetMin = firstNonNull(
                    request == null ? null : request.targetMin(),
                    existing == null ? null : existing.getTargetMin(),
                    goalDO.getDefaultTargetMin()
            );
            targetMax = firstNonNull(
                    request == null ? null : request.targetMax(),
                    existing == null ? null : existing.getTargetMax(),
                    goalDO.getDefaultTargetMax()
            );
            targetText = null;
            if (targetMin != null && targetMax != null && targetMin.compareTo(targetMax) > 0) {
                throw new BizException(4009, "targetMin must be <= targetMax");
            }
        } else {
            targetMin = null;
            targetMax = null;
            targetText = firstNonBlank(
                    request == null ? null : request.targetText(),
                    existing == null ? null : existing.getTargetText(),
                    goalDO.getDefaultTargetText()
            );
            if (!StringUtils.hasText(targetText)) {
                throw new BizException(4009, "targetText is required for text/boolean goal");
            }
        }

        LocalDate startDate = firstNonNull(
                request == null ? null : request.startDate(),
                existing == null ? null : existing.getStartDate(),
                LocalDate.now()
        );
        LocalDate endDate = firstNonNull(
                request == null ? null : request.endDate(),
                existing == null ? null : existing.getEndDate(),
                null
        );
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new BizException(4009, "endDate must be greater than or equal to startDate");
        }

        return new NormalizedSelection(targetMin, targetMax, targetText, startDate, endDate);
    }

    private <T> T firstNonNull(T first, T second, T third) {
        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        return third;
    }

    private String firstNonBlank(String first, String second, String third) {
        String normalizedFirst = normalizeText(first);
        if (StringUtils.hasText(normalizedFirst)) {
            return normalizedFirst;
        }
        String normalizedSecond = normalizeText(second);
        if (StringUtils.hasText(normalizedSecond)) {
            return normalizedSecond;
        }
        return normalizeText(third);
    }

    private Long currentUserId() {
        Long userId = LoginUserHolder.getUserId();
        if (userId == null) {
            throw new BizException(4010, "请先登录", HttpStatus.UNAUTHORIZED);
        }
        return userId;
    }

    private String normalizeText(String value) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return null;
    }

    private HealthGoalCardVO toCardVO(HealthGoalDO goalDO) {
        boolean selected = goalDO.getUserGoalId() != null
                && goalDO.getUserGoalStatus() != null
                && goalDO.getUserGoalStatus() == STATUS_ACTIVE;
        return new HealthGoalCardVO(
                goalDO.getGoalId(),
                goalDO.getGoalCode(),
                goalDO.getGoalName(),
                goalDO.getGoalDescription(),
                goalDO.getMetricType(),
                goalDO.getUnit(),
                goalDO.getDefaultTargetMin(),
                goalDO.getDefaultTargetMax(),
                goalDO.getDefaultTargetText(),
                goalDO.getSortNo(),
                selected,
                goalDO.getUserGoalId(),
                goalDO.getUserGoalStatus(),
                goalDO.getTargetMin(),
                goalDO.getTargetMax(),
                goalDO.getTargetText()
        );
    }

    private UserHealthGoalVO toUserGoalVO(UserHealthGoalDO userGoalDO) {
        return new UserHealthGoalVO(
                userGoalDO.getUserGoalId(),
                userGoalDO.getGoalId(),
                userGoalDO.getGoalCode(),
                userGoalDO.getGoalName(),
                userGoalDO.getGoalDescription(),
                userGoalDO.getMetricType(),
                userGoalDO.getUnit(),
                effectiveTargetMin(userGoalDO),
                effectiveTargetMax(userGoalDO),
                effectiveTargetText(userGoalDO),
                userGoalDO.getStartDate(),
                userGoalDO.getEndDate(),
                userGoalDO.getUserGoalStatus(),
                userGoalDO.getRecordCount() == null ? 0 : userGoalDO.getRecordCount(),
                userGoalDO.getLatestRecordValue(),
                userGoalDO.getLatestRecordText(),
                userGoalDO.getLatestEvaluationResult(),
                userGoalDO.getLatestRecordTime(),
                userGoalDO.getCreatedTime(),
                userGoalDO.getLastChangeTime()
        );
    }

    private BigDecimal effectiveTargetMin(UserHealthGoalDO userGoalDO) {
        return userGoalDO.getTargetMin() != null ? userGoalDO.getTargetMin() : userGoalDO.getDefaultTargetMin();
    }

    private BigDecimal effectiveTargetMax(UserHealthGoalDO userGoalDO) {
        return userGoalDO.getTargetMax() != null ? userGoalDO.getTargetMax() : userGoalDO.getDefaultTargetMax();
    }

    private String effectiveTargetText(UserHealthGoalDO userGoalDO) {
        if (StringUtils.hasText(userGoalDO.getTargetText())) {
            return userGoalDO.getTargetText();
        }
        return userGoalDO.getDefaultTargetText();
    }

    private record NormalizedSelection(
            BigDecimal targetMin,
            BigDecimal targetMax,
            String targetText,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }
}
