package org.hylee.phms.server.service.impl;

import org.hylee.phms.server.context.LoginUserHolder;
import org.hylee.phms.server.dto.CreateHealthRecordRequest;
import org.hylee.phms.server.exception.BizException;
import org.hylee.phms.server.mapper.HealthRecordMapper;
import org.hylee.phms.server.mapper.UserHealthGoalMapper;
import org.hylee.phms.server.persistence.HealthRecordDO;
import org.hylee.phms.server.persistence.UserHealthGoalDO;
import org.hylee.phms.server.service.HealthRecordService;
import org.hylee.phms.server.vo.HealthRecordVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class HealthRecordServiceImpl implements HealthRecordService {

    private static final int STATUS_ACTIVE = 0;
    private static final int DEFAULT_LIMIT = 400;
    private static final int MAX_LIMIT = 1000;

    private final HealthRecordMapper healthRecordMapper;
    private final UserHealthGoalMapper userHealthGoalMapper;

    public HealthRecordServiceImpl(HealthRecordMapper healthRecordMapper,
                                   UserHealthGoalMapper userHealthGoalMapper) {
        this.healthRecordMapper = healthRecordMapper;
        this.userHealthGoalMapper = userHealthGoalMapper;
    }

    @Override
    public List<HealthRecordVO> listRecords(Long userGoalId, Integer rangeDays, Integer limit) {
        Long userId = currentUserId();
        UserHealthGoalDO userGoalDO = requireUserGoal(userGoalId, userId);
        Integer queryLimit = normalizeLimit(limit);
        LocalDateTime rangeStart = resolveRangeStart(rangeDays);
        return healthRecordMapper.selectByUserGoalAndRange(
                        userGoalId,
                        userId,
                        userGoalDO.getGoalId(),
                        rangeStart,
                        queryLimit
                ).stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HealthRecordVO createRecord(Long userGoalId, CreateHealthRecordRequest request) {
        Long userId = currentUserId();
        UserHealthGoalDO userGoalDO = requireUserGoal(userGoalId, userId);
        if (userGoalDO.getUserGoalStatus() == null || userGoalDO.getUserGoalStatus() != STATUS_ACTIVE) {
            throw new BizException(4009, "only active health goal can accept new records");
        }

        NormalizedRecord normalized = normalizeRecord(request, userGoalDO);
        HealthRecordDO recordDO = new HealthRecordDO();
        recordDO.setUserGoalId(userGoalId);
        recordDO.setUserId(userId);
        recordDO.setGoalId(userGoalDO.getGoalId());
        recordDO.setRecordValue(normalized.recordValue());
        recordDO.setRecordText(normalized.recordText());
        recordDO.setRecordTime(request.recordTime());
        recordDO.setRecordSource(request.recordSource() == null ? 0 : request.recordSource());
        recordDO.setEvaluationResult(evaluate(userGoalDO, normalized.recordValue(), normalized.recordText()));
        recordDO.setRemark(normalizeText(request.remark()));
        healthRecordMapper.insert(recordDO);

        HealthRecordDO created = healthRecordMapper.selectByIdAndUser(recordDO.getRecordId(), userId);
        if (created == null) {
            throw new BizException(5005, "failed to load created health record", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return toVO(created);
    }

    private NormalizedRecord normalizeRecord(CreateHealthRecordRequest request, UserHealthGoalDO userGoalDO) {
        Integer metricType = userGoalDO.getMetricType();
        if (metricType != null && metricType == 1) {
            if (request.recordValue() == null) {
                throw new BizException(4009, "recordValue is required for numeric goal");
            }
            return new NormalizedRecord(request.recordValue(), null);
        }

        String recordText = normalizeText(request.recordText());
        if (!StringUtils.hasText(recordText)) {
            throw new BizException(4009, "recordText is required for text/boolean goal");
        }
        if (metricType != null && metricType == 3) {
            recordText = normalizeBooleanText(recordText, true);
        }
        return new NormalizedRecord(null, recordText);
    }

    private Integer evaluate(UserHealthGoalDO userGoalDO, BigDecimal recordValue, String recordText) {
        Integer metricType = userGoalDO.getMetricType();
        if (metricType != null && metricType == 1) {
            BigDecimal targetMin = effectiveTargetMin(userGoalDO);
            BigDecimal targetMax = effectiveTargetMax(userGoalDO);
            if (targetMin == null && targetMax == null) {
                return 0;
            }
            if (targetMin != null && recordValue.compareTo(targetMin) < 0) {
                return 1;
            }
            if (targetMax != null && recordValue.compareTo(targetMax) > 0) {
                return 3;
            }
            return 2;
        }

        String targetText = effectiveTargetText(userGoalDO);
        if (!StringUtils.hasText(targetText)) {
            return 0;
        }
        if (metricType != null && metricType == 3) {
            String normalizedTarget = normalizeBooleanText(targetText, false);
            if (!StringUtils.hasText(normalizedTarget)) {
                return 0;
            }
            return normalizedTarget.equals(recordText) ? 2 : 0;
        }
        return targetText.equalsIgnoreCase(recordText) ? 2 : 0;
    }

    private BigDecimal effectiveTargetMin(UserHealthGoalDO userGoalDO) {
        return userGoalDO.getTargetMin() != null ? userGoalDO.getTargetMin() : userGoalDO.getDefaultTargetMin();
    }

    private BigDecimal effectiveTargetMax(UserHealthGoalDO userGoalDO) {
        return userGoalDO.getTargetMax() != null ? userGoalDO.getTargetMax() : userGoalDO.getDefaultTargetMax();
    }

    private String effectiveTargetText(UserHealthGoalDO userGoalDO) {
        if (StringUtils.hasText(userGoalDO.getTargetText())) {
            return userGoalDO.getTargetText().trim();
        }
        if (StringUtils.hasText(userGoalDO.getDefaultTargetText())) {
            return userGoalDO.getDefaultTargetText().trim();
        }
        return null;
    }

    private String normalizeBooleanText(String value, boolean failOnUnknown) {
        String normalized = normalizeText(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        String lowered = normalized.toLowerCase(Locale.ROOT);
        if (List.of("true", "1", "yes", "y", "是", "达成", "已完成").contains(lowered)) {
            return "true";
        }
        if (List.of("false", "0", "no", "n", "否", "未达成", "未完成").contains(lowered)) {
            return "false";
        }
        if (failOnUnknown) {
            throw new BizException(4009, "boolean goal recordText must be true/false or 是/否");
        }
        return null;
    }

    private LocalDateTime resolveRangeStart(Integer rangeDays) {
        if (rangeDays == null) {
            return null;
        }
        LocalDate startDate = LocalDate.now().minusDays(rangeDays.longValue() - 1L);
        return startDate.atStartOfDay();
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

    private UserHealthGoalDO requireUserGoal(Long userGoalId, Long userId) {
        UserHealthGoalDO userGoalDO = userHealthGoalMapper.selectByIdAndUser(userGoalId, userId);
        if (userGoalDO == null) {
            throw new BizException(4046, "user health goal not found: " + userGoalId, HttpStatus.NOT_FOUND);
        }
        return userGoalDO;
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

    private HealthRecordVO toVO(HealthRecordDO recordDO) {
        return new HealthRecordVO(
                recordDO.getRecordId(),
                recordDO.getUserGoalId(),
                recordDO.getGoalId(),
                recordDO.getRecordValue(),
                recordDO.getRecordText(),
                recordDO.getRecordTime(),
                recordDO.getRecordSource(),
                recordDO.getEvaluationResult(),
                recordDO.getRemark(),
                recordDO.getCreatedTime(),
                recordDO.getLastChangeTime()
        );
    }

    private record NormalizedRecord(BigDecimal recordValue, String recordText) {
    }
}
