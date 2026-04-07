package org.hylee.phms.server.service.impl;

import org.hylee.phms.pojo.model.ExerciseRecord;
import org.hylee.phms.server.context.LoginUserHolder;
import org.hylee.phms.server.dto.CreateExerciseRecordRequest;
import org.hylee.phms.server.dto.UpdateExerciseRecordRequest;
import org.hylee.phms.server.exception.BizException;
import org.hylee.phms.server.mapper.ExerciseRecordMapper;
import org.hylee.phms.server.persistence.ExerciseRecordDO;
import org.hylee.phms.server.service.ExerciseRecordService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * {@link org.hylee.phms.server.service.ExerciseRecordService} 实现（按当前登录用户隔离数据）。
 */
@Service
public class ExerciseRecordServiceImpl implements ExerciseRecordService {

    private static final String SOURCE_MANUAL = "manual";
    private static final Set<String> ALLOWED_DATA_SOURCES = Set.of(SOURCE_MANUAL, "device", "3rd", "import");
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final ExerciseRecordMapper exerciseRecordMapper;

    public ExerciseRecordServiceImpl(ExerciseRecordMapper exerciseRecordMapper) {
        this.exerciseRecordMapper = exerciseRecordMapper;
    }

    @Override
    public ExerciseRecord createRecord(CreateExerciseRecordRequest request) {
        Long userId = currentUserId();
        ExerciseRecordDO recordDO = toCreateDO(userId, request);
        try {
            exerciseRecordMapper.insert(recordDO);
        } catch (DuplicateKeyException ex) {
            throw duplicateExternalRecordError();
        }
        ExerciseRecordDO created = exerciseRecordMapper.selectByIdAndUser(recordDO.getRecordId(), userId);
        if (created == null) {
            throw new BizException(5001, "failed to load created exercise record", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return toModel(created);
    }

    @Override
    public ExerciseRecord updateRecord(Long recordId, UpdateExerciseRecordRequest request) {
        Long userId = currentUserId();
        ExerciseRecordDO existing = exerciseRecordMapper.selectByIdAndUser(recordId, userId);
        if (existing == null) {
            throw recordNotFound(recordId);
        }

        ExerciseRecordDO toUpdate = toUpdateDO(recordId, userId, request);
        try {
            int affected = exerciseRecordMapper.updateByIdAndUser(toUpdate);
            if (affected == 0) {
                throw recordNotFound(recordId);
            }
        } catch (DuplicateKeyException ex) {
            throw duplicateExternalRecordError();
        }

        ExerciseRecordDO updated = exerciseRecordMapper.selectByIdAndUser(recordId, userId);
        if (updated == null) {
            throw recordNotFound(recordId);
        }
        return toModel(updated);
    }

    @Override
    public void deleteRecord(Long recordId) {
        Long userId = currentUserId();
        int affected = exerciseRecordMapper.deleteByIdAndUser(recordId, userId);
        if (affected == 0) {
            throw recordNotFound(recordId);
        }
    }

    @Override
    public ExerciseRecord getRecord(Long recordId) {
        Long userId = currentUserId();
        ExerciseRecordDO recordDO = exerciseRecordMapper.selectByIdAndUser(recordId, userId);
        if (recordDO == null) {
            throw recordNotFound(recordId);
        }
        return toModel(recordDO);
    }

    @Override
    public List<ExerciseRecord> listRecords(LocalDateTime startTime, LocalDateTime endTime, Integer limit) {
        validateTimeRange(startTime, endTime);
        Long userId = currentUserId();
        Integer queryLimit = normalizeLimit(limit);
        return exerciseRecordMapper.selectByUserAndRange(userId, startTime, endTime, queryLimit).stream()
                .map(this::toModel)
                .toList();
    }

    private ExerciseRecordDO toCreateDO(Long userId, CreateExerciseRecordRequest request) {
        ExerciseRecordDO recordDO = new ExerciseRecordDO();
        recordDO.setUserId(userId);
        recordDO.setSportId(request.sportId());
        recordDO.setSportName(resolveSportName(request.sportId(), request.sportName()));
        recordDO.setRecordTime(request.recordTime());
        recordDO.setDurationMin(request.durationMin());
        recordDO.setCaloriesKcal(request.caloriesKcal());
        recordDO.setNote(normalizeText(request.note()));
        recordDO.setDataSource(resolveDataSource(request.dataSource()));
        recordDO.setExternalId(normalizeText(request.externalId()));
        return recordDO;
    }

    private ExerciseRecordDO toUpdateDO(Long recordId, Long userId, UpdateExerciseRecordRequest request) {
        ExerciseRecordDO recordDO = new ExerciseRecordDO();
        recordDO.setRecordId(recordId);
        recordDO.setUserId(userId);
        recordDO.setSportId(request.sportId());
        recordDO.setSportName(resolveSportName(request.sportId(), request.sportName()));
        recordDO.setRecordTime(request.recordTime());
        recordDO.setDurationMin(request.durationMin());
        recordDO.setCaloriesKcal(request.caloriesKcal());
        recordDO.setNote(normalizeText(request.note()));
        recordDO.setDataSource(resolveDataSource(request.dataSource()));
        recordDO.setExternalId(normalizeText(request.externalId()));
        return recordDO;
    }

    private ExerciseRecord toModel(ExerciseRecordDO recordDO) {
        return new ExerciseRecord(
                recordDO.getRecordId(),
                recordDO.getSportId(),
                recordDO.getSportName(),
                recordDO.getRecordTime(),
                recordDO.getDurationMin(),
                recordDO.getCaloriesKcal(),
                recordDO.getNote(),
                recordDO.getDataSource(),
                recordDO.getExternalId(),
                recordDO.getCreatedTime(),
                recordDO.getLastChangeTime()
        );
    }

    private Long currentUserId() {
        Long userId = LoginUserHolder.getUserId();
        if (userId == null) {
            throw new BizException(4010, "please login first", HttpStatus.UNAUTHORIZED);
        }
        return userId;
    }

    private String resolveSportName(Long sportId, String sportName) {
        if (sportId != null) {
            return null;
        }
        String normalized = normalizeText(sportName);
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(4005, "sportName is required when sportId is null");
        }
        return normalized;
    }

    private String resolveDataSource(String dataSource) {
        String normalized = normalizeText(dataSource);
        if (!StringUtils.hasText(normalized)) {
            return SOURCE_MANUAL;
        }
        String lowered = normalized.toLowerCase(Locale.ROOT);
        if (!ALLOWED_DATA_SOURCES.contains(lowered)) {
            throw new BizException(4006, "dataSource must be one of: manual/device/3rd/import");
        }
        return lowered;
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

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new BizException(4007, "endTime must be greater than or equal to startTime");
        }
    }

    private String normalizeText(String value) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return null;
    }

    private BizException recordNotFound(Long recordId) {
        return new BizException(4042, "exercise record not found: " + recordId, HttpStatus.NOT_FOUND);
    }

    private BizException duplicateExternalRecordError() {
        return new BizException(4091, "duplicate dataSource + externalId", HttpStatus.CONFLICT);
    }
}
