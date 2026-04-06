package org.hylee.phms.server.service.impl;

import org.hylee.phms.server.context.LoginUserHolder;
import org.hylee.phms.server.dto.AdminSaveHealthGoalRequest;
import org.hylee.phms.server.exception.BizException;
import org.hylee.phms.server.mapper.HealthGoalMapper;
import org.hylee.phms.server.persistence.HealthGoalDO;
import org.hylee.phms.server.service.AdminHealthGoalService;
import org.hylee.phms.server.vo.AdminHealthGoalVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
public class AdminHealthGoalServiceImpl implements AdminHealthGoalService {

    private final HealthGoalMapper healthGoalMapper;

    public AdminHealthGoalServiceImpl(HealthGoalMapper healthGoalMapper) {
        this.healthGoalMapper = healthGoalMapper;
    }

    @Override
    public List<AdminHealthGoalVO> listGoals(String keyword, Integer goalStatus) {
        String normalizedKeyword = normalizeText(keyword);
        return healthGoalMapper.selectAdminGoals(normalizedKeyword, goalStatus).stream()
                .map(this::toAdminVO)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminHealthGoalVO createGoal(AdminSaveHealthGoalRequest request) {
        NormalizedGoal normalized = normalizeRequest(request, null);
        HealthGoalDO goalDO = new HealthGoalDO();
        fillGoalDO(goalDO, normalized);
        goalDO.setCreatedBy(currentUserId());
        healthGoalMapper.insert(goalDO);
        return toAdminVO(requireGoal(goalDO.getGoalId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminHealthGoalVO updateGoal(Long goalId, AdminSaveHealthGoalRequest request) {
        requireGoal(goalId);
        NormalizedGoal normalized = normalizeRequest(request, goalId);
        HealthGoalDO goalDO = new HealthGoalDO();
        goalDO.setGoalId(goalId);
        fillGoalDO(goalDO, normalized);
        int affected = healthGoalMapper.updateById(goalDO);
        if (affected == 0) {
            throw goalNotFound(goalId);
        }
        return toAdminVO(requireGoal(goalId));
    }

    private void fillGoalDO(HealthGoalDO goalDO, NormalizedGoal normalized) {
        goalDO.setGoalCode(normalized.goalCode());
        goalDO.setGoalName(normalized.goalName());
        goalDO.setGoalDescription(normalized.goalDescription());
        goalDO.setMetricType(normalized.metricType());
        goalDO.setUnit(normalized.unit());
        goalDO.setDefaultTargetMin(normalized.defaultTargetMin());
        goalDO.setDefaultTargetMax(normalized.defaultTargetMax());
        goalDO.setDefaultTargetText(normalized.defaultTargetText());
        goalDO.setSortNo(normalized.sortNo());
        goalDO.setGoalStatus(normalized.goalStatus());
    }

    private NormalizedGoal normalizeRequest(AdminSaveHealthGoalRequest request, Long excludeGoalId) {
        String goalCode = normalizeGoalCode(request.goalCode());
        String goalName = requireText(request.goalName(), "goalName is required");
        String goalDescription = normalizeText(request.goalDescription());
        Integer metricType = request.metricType();
        Integer sortNo = request.sortNo() == null ? 0 : request.sortNo();
        Integer goalStatus = request.goalStatus() == null ? 0 : request.goalStatus();

        Long duplicateCount = healthGoalMapper.countByCode(goalCode, excludeGoalId);
        if (duplicateCount != null && duplicateCount > 0) {
            throw new BizException(4094, "health goal code already exists: " + goalCode, HttpStatus.CONFLICT);
        }

        BigDecimal targetMin = request.defaultTargetMin();
        BigDecimal targetMax = request.defaultTargetMax();
        String targetText = normalizeText(request.defaultTargetText());
        String unit = normalizeText(request.unit());

        if (metricType == 1) {
            if (targetMin != null && targetMax != null && targetMin.compareTo(targetMax) > 0) {
                throw new BizException(4009, "defaultTargetMin must be <= defaultTargetMax");
            }
            targetText = null;
        } else {
            targetMin = null;
            targetMax = null;
            if (!StringUtils.hasText(targetText)) {
                throw new BizException(4009, "defaultTargetText is required for text/boolean goal");
            }
        }

        return new NormalizedGoal(
                goalCode,
                goalName,
                goalDescription,
                metricType,
                unit,
                targetMin,
                targetMax,
                targetText,
                sortNo,
                goalStatus
        );
    }

    private String normalizeGoalCode(String goalCode) {
        String normalized = requireText(goalCode, "goalCode is required")
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        if (!normalized.matches("^[A-Z0-9_]+$")) {
            throw new BizException(4009, "goalCode only supports letters, digits and underscore");
        }
        return normalized;
    }

    private String requireText(String value, String message) {
        String normalized = normalizeText(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(4009, message);
        }
        return normalized;
    }

    private String normalizeText(String value) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return null;
    }

    private Long currentUserId() {
        Long userId = LoginUserHolder.getUserId();
        if (userId == null) {
            throw new BizException(4010, "请先登录", HttpStatus.UNAUTHORIZED);
        }
        return userId;
    }

    private HealthGoalDO requireGoal(Long goalId) {
        HealthGoalDO goalDO = healthGoalMapper.selectById(goalId);
        if (goalDO == null) {
            throw goalNotFound(goalId);
        }
        return goalDO;
    }

    private BizException goalNotFound(Long goalId) {
        return new BizException(4045, "health goal not found: " + goalId, HttpStatus.NOT_FOUND);
    }

    private AdminHealthGoalVO toAdminVO(HealthGoalDO goalDO) {
        return new AdminHealthGoalVO(
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
                goalDO.getGoalStatus(),
                goalDO.getCreatedBy(),
                goalDO.getCreatorAccount(),
                goalDO.getCreatorName(),
                goalDO.getCreatedTime(),
                goalDO.getLastChangeTime()
        );
    }

    private record NormalizedGoal(
            String goalCode,
            String goalName,
            String goalDescription,
            Integer metricType,
            String unit,
            BigDecimal defaultTargetMin,
            BigDecimal defaultTargetMax,
            String defaultTargetText,
            Integer sortNo,
            Integer goalStatus
    ) {
    }
}
