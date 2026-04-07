package org.hylee.phms.server.service.impl;

import org.hylee.phms.server.context.LoginUserHolder;
import org.hylee.phms.server.dto.CreateUserDataSourceRequest;
import org.hylee.phms.server.dto.UpdateUserDataSourceRequest;
import org.hylee.phms.server.exception.BizException;
import org.hylee.phms.server.mapper.ExerciseRecordMapper;
import org.hylee.phms.server.mapper.HealthMetricMapper;
import org.hylee.phms.server.mapper.SyncTaskMapper;
import org.hylee.phms.server.mapper.UserDataSourceMapper;
import org.hylee.phms.server.persistence.ExerciseRecordDO;
import org.hylee.phms.server.persistence.HealthMetricDO;
import org.hylee.phms.server.persistence.SyncTaskDO;
import org.hylee.phms.server.persistence.UserDataSourceDO;
import org.hylee.phms.server.service.DataSourceService;
import org.hylee.phms.server.vo.DataSourceOverviewVO;
import org.hylee.phms.server.vo.SyncTaskVO;
import org.hylee.phms.server.vo.UserDataSourceVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * {@link org.hylee.phms.server.service.DataSourceService} 实现。
 * <p>
 * 当前用户数据源 CRUD、CSV/文本类文件解析导入（健康指标与运动记录）、同步任务记录与概览聚合；
 * 具体解析规则与字段映射见类内私有方法与常量。
 */
@Service
public class DataSourceServiceImpl implements DataSourceService {

    private static final Set<String> ALLOWED_SOURCE_TYPES = Set.of("file", "manual", "device", "platform");
    private static final int SOURCE_STATUS_ACTIVE = 0;
    private static final int SOURCE_STATUS_PAUSED = 1;
    private static final int SOURCE_STATUS_ERROR = 2;
    private static final int TASK_STATUS_SUCCESS = 0;
    private static final int TASK_STATUS_PARTIAL = 1;
    private static final int TASK_STATUS_FAILED = 2;
    private static final int TASK_STATUS_RUNNING = 3;
    private static final int DEFAULT_TASK_LIMIT = 10;
    private static final int MAX_TASK_LIMIT = 50;
    private static final String TASK_TYPE_IMPORT = "import";
    private static final String METRIC_CATEGORY_HEALTH = "health_metric";
    private static final String METRIC_CATEGORY_EXERCISE = "exercise_record";
    private static final String SOURCE_TYPE_FILE = "file";
    private static final String EXERCISE_DATA_SOURCE = "import";
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd")
    );
    private static final List<DateTimeFormatter> TIME_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_TIME,
            DateTimeFormatter.ofPattern("H:mm"),
            DateTimeFormatter.ofPattern("H:mm:ss")
    );
    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-M-d H:mm"),
            DateTimeFormatter.ofPattern("yyyy-M-d H:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/M/d H:mm"),
            DateTimeFormatter.ofPattern("yyyy/M/d H:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd H:mm"),
            DateTimeFormatter.ofPattern("yyyy.MM.dd H:mm:ss")
    );

    private final UserDataSourceMapper userDataSourceMapper;
    private final SyncTaskMapper syncTaskMapper;
    private final HealthMetricMapper healthMetricMapper;
    private final ExerciseRecordMapper exerciseRecordMapper;

    public DataSourceServiceImpl(UserDataSourceMapper userDataSourceMapper,
                                 SyncTaskMapper syncTaskMapper,
                                 HealthMetricMapper healthMetricMapper,
                                 ExerciseRecordMapper exerciseRecordMapper) {
        this.userDataSourceMapper = userDataSourceMapper;
        this.syncTaskMapper = syncTaskMapper;
        this.healthMetricMapper = healthMetricMapper;
        this.exerciseRecordMapper = exerciseRecordMapper;
    }

    @Override
    public List<UserDataSourceVO> listSources() {
        return userDataSourceMapper.selectByUser(currentUserId()).stream().map(this::toSourceVO).toList();
    }

    @Override
    public UserDataSourceVO createSource(CreateUserDataSourceRequest request) {
        Long userId = currentUserId();
        UserDataSourceDO sourceDO = new UserDataSourceDO();
        sourceDO.setUserId(userId);
        sourceDO.setSourceName(normalizeRequiredText(request.sourceName(), "数据源名称不能为空", 64));
        sourceDO.setSourceType(resolveSourceType(request.sourceType()));
        sourceDO.setSourceStatus(SOURCE_STATUS_ACTIVE);
        sourceDO.setDescription(normalizeText(request.description()));
        try {
            userDataSourceMapper.insert(sourceDO);
        } catch (DuplicateKeyException ex) {
            throw new BizException(4093, "同名数据源已存在", HttpStatus.CONFLICT);
        }
        UserDataSourceDO created = userDataSourceMapper.selectByIdAndUser(sourceDO.getSourceId(), userId);
        if (created == null) {
            throw new BizException(5001, "创建数据源后读取失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return toSourceVO(created);
    }

    @Override
    public UserDataSourceVO updateSource(Long sourceId, UpdateUserDataSourceRequest request) {
        Long userId = currentUserId();
        UserDataSourceDO existing = requireSource(sourceId, userId);
        existing.setSourceName(normalizeRequiredText(request.sourceName(), "数据源名称不能为空", 64));
        existing.setSourceType(resolveSourceType(request.sourceType()));
        existing.setDescription(normalizeText(request.description()));
        try {
            int affected = userDataSourceMapper.updateByIdAndUser(existing);
            if (affected == 0) {
                throw sourceNotFound(sourceId);
            }
        } catch (DuplicateKeyException ex) {
            throw new BizException(4093, "同名数据源已存在", HttpStatus.CONFLICT);
        }
        UserDataSourceDO updated = userDataSourceMapper.selectByIdAndUser(sourceId, userId);
        if (updated == null) {
            throw sourceNotFound(sourceId);
        }
        return toSourceVO(updated);
    }

    @Override
    public UserDataSourceVO updateSourceStatus(Long sourceId, Integer sourceStatus) {
        Long userId = currentUserId();
        validateSourceStatus(sourceStatus);
        requireSource(sourceId, userId);
        int affected = userDataSourceMapper.updateStatusByIdAndUser(sourceId, userId, sourceStatus);
        if (affected == 0) {
            throw sourceNotFound(sourceId);
        }
        UserDataSourceDO updated = userDataSourceMapper.selectByIdAndUser(sourceId, userId);
        if (updated == null) {
            throw sourceNotFound(sourceId);
        }
        return toSourceVO(updated);
    }

    @Override
    @Transactional
    public void deleteSource(Long sourceId) {
        Long userId = currentUserId();
        requireSource(sourceId, userId);
        syncTaskMapper.deleteBySourceIdAndUser(sourceId, userId);
        healthMetricMapper.clearSourceReferenceByUserAndSource(userId, sourceId);
        int affected = userDataSourceMapper.deleteByIdAndUser(sourceId, userId);
        if (affected == 0) {
            throw sourceNotFound(sourceId);
        }
    }

    @Override
    @Transactional
    public SyncTaskVO importHealthMetrics(Long sourceId, MultipartFile file) {
        Long userId = currentUserId();
        UserDataSourceDO source = validateImportSource(sourceId, userId);
        validateUploadFile(file);
        SyncTaskDO taskDO = beginImportTask(userId, sourceId, file, METRIC_CATEGORY_HEALTH);
        ImportStats stats = new ImportStats();
        List<String> errors = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            parseAndImportHealthMetrics(userId, source, taskDO.getTaskId(), reader, stats, errors);
        } catch (IOException ex) {
            stats.failCount += 1;
            collectError(errors, "读取上传文件失败");
        }
        return finishImportTask(userId, source, taskDO, stats, errors);
    }

    @Override
    @Transactional
    public SyncTaskVO importExerciseRecords(Long sourceId, MultipartFile file) {
        Long userId = currentUserId();
        UserDataSourceDO source = validateImportSource(sourceId, userId);
        validateUploadFile(file);
        SyncTaskDO taskDO = beginImportTask(userId, sourceId, file, METRIC_CATEGORY_EXERCISE);
        ImportStats stats = new ImportStats();
        List<String> errors = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            parseAndImportExerciseRecords(userId, source, reader, stats, errors);
        } catch (IOException ex) {
            stats.failCount += 1;
            collectError(errors, "读取上传文件失败");
        }
        return finishImportTask(userId, source, taskDO, stats, errors);
    }

    @Override
    public List<SyncTaskVO> listSyncTasks(Long sourceId, Integer limit) {
        Long userId = currentUserId();
        Integer queryLimit = normalizeTaskLimit(limit);
        if (sourceId != null) {
            requireSource(sourceId, userId);
        }
        return syncTaskMapper.selectRecentByUser(userId, sourceId, queryLimit).stream().map(this::toTaskVO).toList();
    }

    @Override
    public DataSourceOverviewVO getOverview() {
        Long userId = currentUserId();
        SyncTaskDO latestTask = syncTaskMapper.selectLatestByUser(userId);
        return new DataSourceOverviewVO(
                userDataSourceMapper.countByUser(userId),
                userDataSourceMapper.countActiveByUser(userId),
                healthMetricMapper.countImportedMetricDays(userId),
                exerciseRecordMapper.countImportedByUser(userId),
                syncTaskMapper.countByUser(userId),
                latestTask == null ? null : toTaskVO(latestTask)
        );
    }

    private void parseAndImportHealthMetrics(Long userId,
                                             UserDataSourceDO source,
                                             Long taskId,
                                             BufferedReader reader,
                                             ImportStats stats,
                                             List<String> errors) throws IOException {
        CsvContent csvContent = readCsvContent(reader);
        if (csvContent.headerLine() == null) {
            stats.failCount += 1;
            collectError(errors, "CSV 缺少表头");
            return;
        }
        Map<String, Integer> headerIndex = resolveHealthHeaderIndex(parseCsvLine(csvContent.headerLine()));
        if (!headerIndex.containsKey("measureDate")) {
            stats.failCount += 1;
            collectError(errors, "表头缺少测量日期列");
            return;
        }
        for (CsvLine csvLine : csvContent.dataLines()) {
            stats.totalCount += 1;
            try {
                ImportedMetricRow row = parseMetricRow(headerIndex, parseCsvLine(csvLine.content()));
                saveImportedMetric(userId, source, taskId, row, stats);
            } catch (IllegalArgumentException ex) {
                stats.failCount += 1;
                collectError(errors, "第 " + csvLine.lineNumber() + " 行：" + ex.getMessage());
            }
        }
        if (stats.totalCount == 0 && stats.failCount == 0) {
            stats.failCount += 1;
            collectError(errors, "CSV 中没有可导入的数据行");
        }
    }

    private void parseAndImportExerciseRecords(Long userId,
                                               UserDataSourceDO source,
                                               BufferedReader reader,
                                               ImportStats stats,
                                               List<String> errors) throws IOException {
        CsvContent csvContent = readCsvContent(reader);
        if (csvContent.headerLine() == null) {
            stats.failCount += 1;
            collectError(errors, "CSV 缺少表头");
            return;
        }
        Map<String, Integer> headerIndex = resolveExerciseHeaderIndex(parseCsvLine(csvContent.headerLine()));
        if (!headerIndex.containsKey("recordTime") && !headerIndex.containsKey("recordDate")) {
            stats.failCount += 1;
            collectError(errors, "表头缺少记录时间列");
            return;
        }
        if (!headerIndex.containsKey("durationMin")) {
            stats.failCount += 1;
            collectError(errors, "表头缺少时长列");
            return;
        }
        for (CsvLine csvLine : csvContent.dataLines()) {
            stats.totalCount += 1;
            try {
                ImportedExerciseRow row = parseExerciseRow(headerIndex, parseCsvLine(csvLine.content()));
                saveImportedExercise(userId, source, row, stats);
            } catch (IllegalArgumentException ex) {
                stats.failCount += 1;
                collectError(errors, "第 " + csvLine.lineNumber() + " 行：" + ex.getMessage());
            }
        }
        if (stats.totalCount == 0 && stats.failCount == 0) {
            stats.failCount += 1;
            collectError(errors, "CSV 中没有可导入的数据行");
        }
    }

    private CsvContent readCsvContent(BufferedReader reader) throws IOException {
        String headerLine = null;
        List<CsvLine> dataLines = new ArrayList<>();
        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
            lineNumber += 1;
            String normalized = stripBom(line).trim();
            if (!StringUtils.hasText(normalized)) {
                continue;
            }
            if (headerLine == null) {
                headerLine = normalized;
            } else {
                dataLines.add(new CsvLine(lineNumber, normalized));
            }
        }
        return new CsvContent(headerLine, dataLines);
    }

    private void saveImportedMetric(Long userId, UserDataSourceDO source, Long taskId, ImportedMetricRow row, ImportStats stats) {
        HealthMetricDO existing = healthMetricMapper.selectByUserAndDate(userId, row.measureDate());
        HealthMetricDO target = new HealthMetricDO();
        target.setUserId(userId);
        target.setMeasureDate(row.measureDate());
        target.setSteps(mergeInteger(existing == null ? null : existing.getSteps(), row.steps()));
        target.setRestingHeartRate(mergeInteger(existing == null ? null : existing.getRestingHeartRate(), row.restingHeartRate()));
        target.setSleepHours(mergeDouble(existing == null ? null : existing.getSleepHours(), row.sleepHours()));
        target.setSystolic(mergeInteger(existing == null ? null : existing.getSystolic(), row.systolic()));
        target.setDiastolic(mergeInteger(existing == null ? null : existing.getDiastolic(), row.diastolic()));
        target.setStressLevel(mergeInteger(existing == null ? null : existing.getStressLevel(), row.stressLevel()));
        target.setSourceId(source.getSourceId());
        target.setSourceType(source.getSourceType());
        target.setSourceName(source.getSourceName());
        target.setSyncTaskId(taskId);
        if (!hasAnyMetricValue(target)) {
            throw new IllegalArgumentException("至少需要填写一项健康指标");
        }
        if (existing == null) {
            healthMetricMapper.insert(target);
            stats.insertCount += 1;
        } else {
            healthMetricMapper.updateByUserAndDate(target);
            stats.updateCount += 1;
        }
    }

    private void saveImportedExercise(Long userId, UserDataSourceDO source, ImportedExerciseRow row, ImportStats stats) {
        String externalId = buildExerciseExternalId(userId, source.getSourceId(), row);
        ExerciseRecordDO existing = exerciseRecordMapper.selectByUserAndExternalKey(userId, EXERCISE_DATA_SOURCE, externalId);
        ExerciseRecordDO target = new ExerciseRecordDO();
        target.setUserId(userId);
        target.setSportId(row.sportId());
        target.setSportName(resolveSportName(row.sportId(), row.sportName()));
        target.setRecordTime(row.recordTime());
        target.setDurationMin(row.durationMin());
        target.setCaloriesKcal(row.caloriesKcal());
        target.setNote(row.note());
        target.setDataSource(EXERCISE_DATA_SOURCE);
        target.setExternalId(externalId);
        try {
            if (existing == null) {
                exerciseRecordMapper.insert(target);
                stats.insertCount += 1;
            } else {
                target.setRecordId(existing.getRecordId());
                exerciseRecordMapper.updateByIdAndUser(target);
                stats.updateCount += 1;
            }
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("检测到重复的运动记录标识");
        }
    }

    private Map<String, Integer> resolveHealthHeaderIndex(List<String> headers) {
        Map<String, Integer> headerIndex = new HashMap<>();
        for (int index = 0; index < headers.size(); index += 1) {
            String header = normalizeHeader(headers.get(index));
            if (matches(header, "measuredate", "measure_date", "date", "日期", "测量日期")) headerIndex.put("measureDate", index);
            else if (matches(header, "steps", "step", "步数")) headerIndex.put("steps", index);
            else if (matches(header, "restingheartrate", "resting_heart_rate", "restinghr", "heart_rate", "静息心率", "心率")) headerIndex.put("restingHeartRate", index);
            else if (matches(header, "sleephours", "sleep_hours", "sleep", "睡眠时长", "睡眠")) headerIndex.put("sleepHours", index);
            else if (matches(header, "systolic", "收缩压")) headerIndex.put("systolic", index);
            else if (matches(header, "diastolic", "舒张压")) headerIndex.put("diastolic", index);
            else if (matches(header, "stresslevel", "stress_level", "stress", "压力指数", "压力")) headerIndex.put("stressLevel", index);
        }
        return headerIndex;
    }

    private Map<String, Integer> resolveExerciseHeaderIndex(List<String> headers) {
        Map<String, Integer> headerIndex = new HashMap<>();
        for (int index = 0; index < headers.size(); index += 1) {
            String header = normalizeHeader(headers.get(index));
            if (matches(header, "recordtime", "record_time", "datetime", "记录时间", "运动时间", "开始时间")) headerIndex.put("recordTime", index);
            else if (matches(header, "recorddate", "record_date", "date", "日期", "记录日期", "运动日期")) headerIndex.put("recordDate", index);
            else if (matches(header, "recordclock", "record_clock", "time", "时间", "时刻")) headerIndex.put("recordClock", index);
            else if (matches(header, "sportid", "sport_id", "courseid", "course_id", "课程id", "运动id")) headerIndex.put("sportId", index);
            else if (matches(header, "sportname", "sport_name", "sport", "coursename", "course_name", "运动名称", "课程名称", "项目名称")) headerIndex.put("sportName", index);
            else if (matches(header, "durationmin", "duration_min", "duration", "durationminutes", "minutes", "时长", "时长分钟", "分钟")) headerIndex.put("durationMin", index);
            else if (matches(header, "calorieskcal", "calories_kcal", "calories", "kcal", "热量", "消耗热量", "卡路里")) headerIndex.put("caloriesKcal", index);
            else if (matches(header, "note", "remark", "memo", "备注", "说明")) headerIndex.put("note", index);
            else if (matches(header, "externalid", "external_id", "recordid", "外部id", "记录id", "源记录id")) headerIndex.put("externalId", index);
        }
        return headerIndex;
    }

    private ImportedMetricRow parseMetricRow(Map<String, Integer> headerIndex, List<String> columns) {
        return new ImportedMetricRow(
                parseDate(valueAt(columns, headerIndex.get("measureDate")), "测量日期"),
                parseOptionalInteger(valueAt(columns, headerIndex.get("steps")), "步数", 0, 300000),
                parseOptionalInteger(valueAt(columns, headerIndex.get("restingHeartRate")), "静息心率", 20, 220),
                parseOptionalDouble(valueAt(columns, headerIndex.get("sleepHours")), "睡眠时长", 0D, 24D),
                parseOptionalInteger(valueAt(columns, headerIndex.get("systolic")), "收缩压", 50, 250),
                parseOptionalInteger(valueAt(columns, headerIndex.get("diastolic")), "舒张压", 30, 180),
                parseOptionalInteger(valueAt(columns, headerIndex.get("stressLevel")), "压力指数", 0, 100)
        );
    }

    private ImportedExerciseRow parseExerciseRow(Map<String, Integer> headerIndex, List<String> columns) {
        Long sportId = parseOptionalLong(valueAt(columns, headerIndex.get("sportId")), "课程 ID", 1L, 9_999_999_999L);
        String sportName = normalizeLimitedText(valueAt(columns, headerIndex.get("sportName")), 64, "运动名称");
        if (sportId == null && !StringUtils.hasText(sportName)) {
            throw new IllegalArgumentException("至少需要填写运动名称或课程 ID");
        }
        return new ImportedExerciseRow(
                parseExerciseRecordTime(headerIndex, columns),
                sportId,
                sportName,
                parseRequiredInteger(valueAt(columns, headerIndex.get("durationMin")), "时长", 1, 1440),
                parseOptionalDecimal(valueAt(columns, headerIndex.get("caloriesKcal")), "热量", BigDecimal.ZERO, new BigDecimal("50000")),
                normalizeLimitedText(valueAt(columns, headerIndex.get("note")), 255, "备注"),
                normalizeLimitedText(valueAt(columns, headerIndex.get("externalId")), 128, "外部记录 ID")
        );
    }

    private LocalDateTime parseExerciseRecordTime(Map<String, Integer> headerIndex, List<String> columns) {
        String recordTime = valueAt(columns, headerIndex.get("recordTime"));
        if (StringUtils.hasText(recordTime)) {
            return parseDateTime(recordTime, "记录时间");
        }
        String recordDate = valueAt(columns, headerIndex.get("recordDate"));
        if (!StringUtils.hasText(recordDate)) {
            throw new IllegalArgumentException("缺少记录时间或日期");
        }
        String recordClock = valueAt(columns, headerIndex.get("recordClock"));
        LocalTime clock = StringUtils.hasText(recordClock) ? parseTime(recordClock, "记录时刻") : LocalTime.MIDNIGHT;
        return LocalDateTime.of(parseDate(recordDate, "记录日期"), clock);
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        boolean inQuotes = false;
        for (int index = 0; index < line.length(); index += 1) {
            char current = line.charAt(index);
            if (current == '"') {
                if (inQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    builder.append('"');
                    index += 1;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }
            if (current == ',' && !inQuotes) {
                values.add(builder.toString().trim());
                builder.setLength(0);
            } else {
                builder.append(current);
            }
        }
        values.add(builder.toString().trim());
        return values;
    }

    private boolean matches(String actual, String... expectedValues) {
        for (String expected : expectedValues) {
            if (expected.equals(actual)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeHeader(String value) {
        return String.valueOf(value).trim().toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
    }

    private String valueAt(List<String> columns, Integer index) {
        if (index == null || index < 0 || index >= columns.size()) {
            return null;
        }
        String value = columns.get(index);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private LocalDate parseDate(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(value.trim(), formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new IllegalArgumentException(fieldName + "格式应为 yyyy-MM-dd");
    }

    private LocalTime parseTime(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        for (DateTimeFormatter formatter : TIME_FORMATTERS) {
            try {
                return LocalTime.parse(value.trim(), formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new IllegalArgumentException(fieldName + "格式应为 HH:mm 或 HH:mm:ss");
    }

    private LocalDateTime parseDateTime(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        String normalized = value.trim();
        try {
            return LocalDateTime.parse(normalized.replace(" ", "T"));
        } catch (DateTimeParseException ignored) {
        }
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new IllegalArgumentException(fieldName + "格式应为 yyyy-MM-dd HH:mm 或 yyyy-MM-dd HH:mm:ss");
    }

    private Integer parseRequiredInteger(String value, String fieldName, int minValue, int maxValue) {
        Integer parsed = parseOptionalInteger(value, fieldName, minValue, maxValue);
        if (parsed == null) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return parsed;
    }

    private Integer parseOptionalInteger(String value, String fieldName, int minValue, int maxValue) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < minValue || parsed > maxValue) {
                throw new IllegalArgumentException(fieldName + "必须介于 " + minValue + " 到 " + maxValue + " 之间");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + "必须为整数");
        }
    }

    private Long parseOptionalLong(String value, String fieldName, long minValue, long maxValue) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            long parsed = Long.parseLong(value.trim());
            if (parsed < minValue || parsed > maxValue) {
                throw new IllegalArgumentException(fieldName + "必须介于 " + minValue + " 到 " + maxValue + " 之间");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + "必须为整数");
        }
    }

    private Double parseOptionalDouble(String value, String fieldName, double minValue, double maxValue) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            double parsed = Double.parseDouble(value.trim());
            if (parsed < minValue || parsed > maxValue) {
                throw new IllegalArgumentException(fieldName + "必须介于 " + minValue + " 到 " + maxValue + " 之间");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + "必须为数字");
        }
    }

    private BigDecimal parseOptionalDecimal(String value, String fieldName, BigDecimal minValue, BigDecimal maxValue) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            BigDecimal parsed = new BigDecimal(value.trim());
            if (parsed.compareTo(minValue) < 0 || parsed.compareTo(maxValue) > 0) {
                throw new IllegalArgumentException(fieldName + "超出允许范围");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + "必须为数字");
        }
    }

    private UserDataSourceDO validateImportSource(Long sourceId, Long userId) {
        UserDataSourceDO source = requireSource(sourceId, userId);
        if (source.getSourceStatus() != null && source.getSourceStatus() == SOURCE_STATUS_PAUSED) {
            throw new BizException(4232, "当前数据源已暂停，请先启用后再导入", HttpStatus.LOCKED);
        }
        if (!SOURCE_TYPE_FILE.equalsIgnoreCase(String.valueOf(source.getSourceType()))) {
            throw new BizException(4009, "仅文件类型数据源支持 CSV 导入");
        }
        return source;
    }

    private void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(4012, "请上传 CSV 文件");
        }
    }

    private SyncTaskDO beginImportTask(Long userId, Long sourceId, MultipartFile file, String metricCategory) {
        SyncTaskDO taskDO = new SyncTaskDO();
        taskDO.setUserId(userId);
        taskDO.setSourceId(sourceId);
        taskDO.setTaskType(TASK_TYPE_IMPORT);
        taskDO.setTaskStatus(TASK_STATUS_RUNNING);
        taskDO.setFileName(normalizeFileName(file.getOriginalFilename()));
        taskDO.setMetricCategory(metricCategory);
        taskDO.setTotalCount(0);
        taskDO.setInsertCount(0);
        taskDO.setUpdateCount(0);
        taskDO.setFailCount(0);
        taskDO.setSummaryMessage("导入执行中");
        taskDO.setStartedTime(LocalDateTime.now());
        syncTaskMapper.insert(taskDO);
        return taskDO;
    }

    private SyncTaskVO finishImportTask(Long userId, UserDataSourceDO source, SyncTaskDO taskDO, ImportStats stats, List<String> errors) {
        taskDO.setTotalCount(stats.totalCount());
        taskDO.setInsertCount(stats.insertCount());
        taskDO.setUpdateCount(stats.updateCount());
        taskDO.setFailCount(stats.failCount());
        taskDO.setTaskStatus(resolveTaskStatus(stats));
        taskDO.setSummaryMessage(buildTaskSummary(stats, errors));
        taskDO.setFinishedTime(LocalDateTime.now());
        syncTaskMapper.updateSummary(taskDO);
        updateSourceSnapshot(source, taskDO);
        SyncTaskDO latest = syncTaskMapper.selectRecentByUser(userId, source.getSourceId(), 1).stream().findFirst().orElse(taskDO);
        latest.setSourceName(source.getSourceName());
        return toTaskVO(latest);
    }

    private void updateSourceSnapshot(UserDataSourceDO source, SyncTaskDO taskDO) {
        if (taskDO.getTaskStatus() == TASK_STATUS_SUCCESS || taskDO.getTaskStatus() == TASK_STATUS_PARTIAL) {
            userDataSourceMapper.updateSyncSnapshot(source.getSourceId(), source.getUserId(), SOURCE_STATUS_ACTIVE, taskDO.getFinishedTime());
        } else {
            userDataSourceMapper.updateSyncSnapshot(source.getSourceId(), source.getUserId(), SOURCE_STATUS_ERROR, source.getLastSyncTime());
        }
    }

    private int resolveTaskStatus(ImportStats stats) {
        int successCount = stats.insertCount() + stats.updateCount();
        if (successCount > 0 && stats.failCount() == 0) return TASK_STATUS_SUCCESS;
        if (successCount > 0) return TASK_STATUS_PARTIAL;
        return TASK_STATUS_FAILED;
    }

    private String buildTaskSummary(ImportStats stats, List<String> errors) {
        String summary = "共 " + stats.totalCount() + " 行，新增 " + stats.insertCount() + " 条，更新 " + stats.updateCount() + " 条，失败 " + stats.failCount() + " 条";
        if (!errors.isEmpty()) {
            summary = summary + "；问题：" + String.join(" | ", errors);
        }
        return summary.length() > 500 ? summary.substring(0, 500) : summary;
    }

    private void collectError(List<String> errors, String message) {
        if (errors.size() < 3) {
            errors.add(message);
        }
    }

    private Integer mergeInteger(Integer existingValue, Integer importedValue) {
        return importedValue != null ? importedValue : existingValue;
    }

    private Double mergeDouble(Double existingValue, Double importedValue) {
        return importedValue != null ? importedValue : existingValue;
    }

    private boolean hasAnyMetricValue(HealthMetricDO metricDO) {
        return metricDO.getSteps() != null || metricDO.getRestingHeartRate() != null || metricDO.getSleepHours() != null
                || metricDO.getSystolic() != null || metricDO.getDiastolic() != null || metricDO.getStressLevel() != null;
    }

    private String resolveSportName(Long sportId, String sportName) {
        if (sportId != null) {
            return null;
        }
        String normalized = normalizeText(sportName);
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException("至少需要填写运动名称或课程 ID");
        }
        return normalized;
    }

    private String buildExerciseExternalId(Long userId, Long sourceId, ImportedExerciseRow row) {
        String rawExternalId = normalizeText(row.externalId());
        String sportKey = row.sportId() != null ? "sportId=" + row.sportId() : "sportName=" + row.sportName();
        String basis = StringUtils.hasText(rawExternalId)
                ? "external:" + rawExternalId
                : "generated:" + row.recordTime() + "|" + sportKey + "|" + row.durationMin();
        return "imp_" + digestText(userId + "|" + sourceId + "|" + basis);
    }

    private String digestText(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < digest.length && builder.length() < 32; index += 1) {
                builder.append(String.format("%02x", digest[index]));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private Integer normalizeTaskLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_TASK_LIMIT;
        }
        if (limit < 1 || limit > MAX_TASK_LIMIT) {
            throw new BizException(4011, "limit 必须在 1 到 " + MAX_TASK_LIMIT + " 之间");
        }
        return limit;
    }

    private String resolveSourceType(String sourceType) {
        String normalized = normalizeRequiredText(sourceType, "数据源类型不能为空", 32).toLowerCase(Locale.ROOT);
        if (!ALLOWED_SOURCE_TYPES.contains(normalized)) {
            throw new BizException(4010, "数据源类型仅支持 file/manual/device/platform");
        }
        return normalized;
    }

    private void validateSourceStatus(Integer sourceStatus) {
        if (sourceStatus == null || sourceStatus < SOURCE_STATUS_ACTIVE || sourceStatus > SOURCE_STATUS_ERROR) {
            throw new BizException(4013, "sourceStatus 只能是 0、1 或 2");
        }
    }

    private UserDataSourceDO requireSource(Long sourceId, Long userId) {
        UserDataSourceDO sourceDO = userDataSourceMapper.selectByIdAndUser(sourceId, userId);
        if (sourceDO == null) {
            throw sourceNotFound(sourceId);
        }
        return sourceDO;
    }

    private Long currentUserId() {
        Long userId = LoginUserHolder.getUserId();
        if (userId == null) {
            throw new BizException(4014, "请先登录", HttpStatus.UNAUTHORIZED);
        }
        return userId;
    }

    private UserDataSourceVO toSourceVO(UserDataSourceDO sourceDO) {
        return new UserDataSourceVO(
                sourceDO.getSourceId(),
                sourceDO.getSourceName(),
                sourceDO.getSourceType(),
                sourceDO.getSourceStatus(),
                sourceDO.getDescription(),
                sourceDO.getLastSyncTime(),
                sourceDO.getMetricCount() == null ? 0L : sourceDO.getMetricCount(),
                sourceDO.getTaskCount() == null ? 0L : sourceDO.getTaskCount(),
                sourceDO.getCreatedTime(),
                sourceDO.getLastChangeTime()
        );
    }

    private SyncTaskVO toTaskVO(SyncTaskDO taskDO) {
        return new SyncTaskVO(
                taskDO.getTaskId(),
                taskDO.getSourceId(),
                taskDO.getSourceName(),
                taskDO.getTaskType(),
                taskDO.getTaskStatus(),
                taskDO.getFileName(),
                taskDO.getMetricCategory(),
                taskDO.getTotalCount(),
                taskDO.getInsertCount(),
                taskDO.getUpdateCount(),
                taskDO.getFailCount(),
                taskDO.getSummaryMessage(),
                taskDO.getStartedTime(),
                taskDO.getFinishedTime()
        );
    }

    private String normalizeRequiredText(String value, String message, int maxLength) {
        String normalized = normalizeText(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(4001, message);
        }
        if (normalized.length() > maxLength) {
            throw new BizException(4002, "文本长度不能超过 " + maxLength + " 个字符");
        }
        return normalized;
    }

    private String normalizeLimitedText(String value, int maxLength, String fieldName) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + "长度不能超过 " + maxLength + " 个字符");
        }
        return normalized;
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeFileName(String originalFilename) {
        String normalized = normalizeText(originalFilename);
        if (!StringUtils.hasText(normalized)) {
            return "upload.csv";
        }
        return normalized.length() > 255 ? normalized.substring(0, 255) : normalized;
    }

    private String stripBom(String value) {
        if (value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }
        return value;
    }

    private BizException sourceNotFound(Long sourceId) {
        return new BizException(4043, "未找到对应的数据源：" + sourceId, HttpStatus.NOT_FOUND);
    }

    private record CsvContent(String headerLine, List<CsvLine> dataLines) {
    }

    private record CsvLine(int lineNumber, String content) {
    }

    private record ImportedMetricRow(LocalDate measureDate,
                                     Integer steps,
                                     Integer restingHeartRate,
                                     Double sleepHours,
                                     Integer systolic,
                                     Integer diastolic,
                                     Integer stressLevel) {
    }

    private record ImportedExerciseRow(LocalDateTime recordTime,
                                       Long sportId,
                                       String sportName,
                                       Integer durationMin,
                                       BigDecimal caloriesKcal,
                                       String note,
                                       String externalId) {
    }

    private static final class ImportStats {
        private int totalCount;
        private int insertCount;
        private int updateCount;
        private int failCount;

        int totalCount() {
            return totalCount;
        }

        int insertCount() {
            return insertCount;
        }

        int updateCount() {
            return updateCount;
        }

        int failCount() {
            return failCount;
        }
    }
}
