package org.hylee.phms.server.service.impl;

import org.hylee.phms.pojo.model.DashboardSummary;
import org.hylee.phms.pojo.model.HealthMetric;
import org.hylee.phms.pojo.model.UserProfile;
import org.hylee.phms.server.context.LoginUserHolder;
import org.hylee.phms.server.dto.UpdateProfileRequest;
import org.hylee.phms.server.exception.BizException;
import org.hylee.phms.server.mapper.HealthMetricMapper;
import org.hylee.phms.server.mapper.UserMapper;
import org.hylee.phms.server.persistence.HealthMetricDO;
import org.hylee.phms.server.persistence.UserDO;
import org.hylee.phms.server.service.HealthDataService;
import org.hylee.phms.server.util.IdCardUtil;
import org.hylee.phms.server.vo.IdCardRecognitionVO;
import org.hylee.phms.server.vo.ProfileDetailVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link org.hylee.phms.server.service.HealthDataService} 实现。
 * <p>
 * 聚合用户档案、健康指标查询、仪表盘汇总及身份证辅助解析等能力。
 */
@Service
public class HealthDataServiceImpl implements HealthDataService {

    private static final int MAX_METRIC_DAYS = 30;

    private final HealthMetricMapper healthMetricMapper;
    private final UserMapper userMapper;

    public HealthDataServiceImpl(HealthMetricMapper healthMetricMapper, UserMapper userMapper) {
        this.healthMetricMapper = healthMetricMapper;
        this.userMapper = userMapper;
    }

    @Override
    public UserProfile getUserProfile() {
        UserDO userDO = currentUser();
        String displayName = StringUtils.hasText(userDO.getNickname()) ? userDO.getNickname() : userDO.getAccount();
        return new UserProfile(
                userDO.getUserId(),
                displayName,
                userDO.getAge(),
                toGenderText(userDO.getGender()),
                null,
                null,
                List.of("基础档案")
        );
    }

    @Override
    public ProfileDetailVO getProfileDetail() {
        return toProfileDetail(currentUser());
    }

    @Override
    public IdCardRecognitionVO recognizeByIdCard(String idcard) {
        String normalized = normalizeText(idcard, null);
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(4004, "身份证号不能为空");
        }
        try {
            IdCardUtil.ParseResult parseResult = IdCardUtil.parse(normalized);
            return new IdCardRecognitionVO(normalized, parseResult.gender(), parseResult.age(), parseResult.birthDate());
        } catch (IllegalArgumentException ex) {
            throw new BizException(4004, ex.getMessage());
        }
    }

    @Override
    public ProfileDetailVO updateProfile(UpdateProfileRequest request) {
        UserDO userDO = currentUser();
        userDO.setNickname(normalizeText(request.nickname(), userDO.getAccount()));
        userDO.setRealname(normalizeText(request.realname(), null));

        String idcard = normalizeIdCard(request.idcard());
        userDO.setIdcard(idcard);
        if (StringUtils.hasText(idcard)) {
            IdCardRecognitionVO recognitionVO = recognizeByIdCard(idcard);
            userDO.setGender(recognitionVO.gender());
            userDO.setAge(recognitionVO.age());
        } else {
            userDO.setGender(request.gender() == null ? 0 : request.gender());
            userDO.setAge(request.age());
        }
        userMapper.updateProfile(userDO);

        UserDO updated = userMapper.selectById(userDO.getUserId());
        return toProfileDetail(updated);
    }

    @Override
    public DashboardSummary getDashboardSummary() {
        List<HealthMetric> recentMetrics = getRecentMetrics(7);
        Long userId = currentUser().getUserId();
        HealthMetricDO latestMetricDO = healthMetricMapper.selectLatestMetricByUser(userId);
        HealthMetric latest = latestMetricDO == null ? null : toMetric(latestMetricDO);
        String advice = latest == null ? "暂无健康数据" : buildAdvice(latest);
        return new DashboardSummary(getUserProfile(), latest, recentMetrics, advice);
    }

    @Override
    public List<HealthMetric> getRecentMetrics(Integer days) {
        int queryDays = (days == null || days < 1) ? 7 : days;
        int boundedDays = Math.min(queryDays, MAX_METRIC_DAYS);
        Long userId = currentUser().getUserId();
        return healthMetricMapper.selectRecentMetrics(
                        userId,
                        java.time.LocalDate.now().minusDays(boundedDays - 1L),
                        boundedDays
                ).stream()
                .map(this::toMetric)
                .toList();
    }

    private UserDO currentUser() {
        Long userId = LoginUserHolder.getUserId();
        if (userId == null) {
            throw new BizException(4010, "请先登录", HttpStatus.UNAUTHORIZED);
        }
        UserDO userDO = userMapper.selectById(userId);
        if (userDO == null) {
            throw new BizException(4011, "登录状态失效，请重新登录", HttpStatus.UNAUTHORIZED);
        }
        return userDO;
    }

    private String toGenderText(Integer gender) {
        if (gender == null || gender == 0) {
            return "未知";
        }
        if (gender == 1) {
            return "男";
        }
        if (gender == 2) {
            return "女";
        }
        return "未知";
    }

    private ProfileDetailVO toProfileDetail(UserDO userDO) {
        return new ProfileDetailVO(
                userDO.getUserId(),
                userDO.getAccount(),
                userDO.getNickname(),
                userDO.getRealname(),
                userDO.getIdcard(),
                userDO.getGender(),
                userDO.getAge(),
                userDO.getAccountStatus()
        );
    }

    private String normalizeText(String value, String fallback) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return fallback;
    }

    private String normalizeIdCard(String idcard) {
        if (StringUtils.hasText(idcard)) {
            return idcard.trim().toUpperCase();
        }
        return null;
    }

    private String buildAdvice(HealthMetric metric) {
        List<String> adviceItems = new ArrayList<>();
        if (metric.steps() != null && metric.steps() < 8000) {
            adviceItems.add("建议增加步行活动");
        }
        if (metric.sleepHours() != null && metric.sleepHours() < 7.0) {
            adviceItems.add("建议保证 7 小时以上睡眠");
        }
        if (metric.stressLevel() != null && metric.stressLevel() > 65) {
            adviceItems.add("建议进行减压训练");
        }
        boolean highSystolic = metric.systolic() != null && metric.systolic() > 130;
        boolean highDiastolic = metric.diastolic() != null && metric.diastolic() > 85;
        if (highSystolic || highDiastolic) {
            adviceItems.add("建议监测血压变化");
        }
        return adviceItems.isEmpty() ? "整体状态良好，建议保持当前节奏。" : String.join("，", adviceItems) + "。";
    }

    private HealthMetric toMetric(HealthMetricDO metricDO) {
        return new HealthMetric(
                metricDO.getMeasureDate(),
                metricDO.getSteps(),
                metricDO.getRestingHeartRate(),
                metricDO.getSleepHours(),
                metricDO.getSystolic(),
                metricDO.getDiastolic(),
                metricDO.getStressLevel(),
                metricDO.getSourceType(),
                metricDO.getSourceName()
        );
    }
}
