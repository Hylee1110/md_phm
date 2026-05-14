package org.hylee.phms.server.service.impl;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.hylee.phms.server.mapper.HealthMetricMapper;
import org.hylee.phms.server.mapper.SyncTaskMapper;
import org.hylee.phms.server.mapper.UserDataSourceMapper;
import org.hylee.phms.server.persistence.HealthMetricDO;
import org.hylee.phms.server.persistence.SyncTaskDO;
import org.hylee.phms.server.persistence.UserDataSourceDO;
import org.hylee.phms.server.service.WatchStreamService;
import org.hylee.phms.server.vo.WatchStreamData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 兼容 hms 项目的手表数据接收实现。
 */
@Service
public class WatchStreamServiceImpl implements WatchStreamService {

    private static final Logger log = LoggerFactory.getLogger(WatchStreamServiceImpl.class);

    private static final String SOURCE_TYPE_DEVICE = "device";
    private static final int SOURCE_STATUS_ACTIVE = 0;
    private static final int SOURCE_STATUS_PAUSED = 1;
    private static final int SOURCE_STATUS_ERROR = 2;
    private static final int TASK_STATUS_SUCCESS = 0;
    private static final int TASK_STATUS_FAILED = 2;
    private static final int TASK_STATUS_RUNNING = 3;
    private static final String TASK_TYPE_SYNC = "sync";
    private static final String METRIC_CATEGORY_HEALTH = "health_metric";

    private final UserDataSourceMapper userDataSourceMapper;
    private final SyncTaskMapper syncTaskMapper;
    private final HealthMetricMapper healthMetricMapper;

    private final AtomicReference<WatchStreamData> latestData = new AtomicReference<>(WatchStreamData.initial());
    private final Set<SseEmitter> sseClients = ConcurrentHashMap.newKeySet();

    private final AtomicLong requestCount = new AtomicLong(0);
    private final AtomicLong ingestCount = new AtomicLong(0);
    private final AtomicLong latestCount = new AtomicLong(0);
    private final AtomicLong eventsOpenCount = new AtomicLong(0);
    private final AtomicLong eventsCloseCount = new AtomicLong(0);
    private final AtomicLong persistSuccessCount = new AtomicLong(0);
    private final AtomicLong persistFailureCount = new AtomicLong(0);
    private final AtomicLong lastRequestAt = new AtomicLong(0);
    private final AtomicLong lastIngestAt = new AtomicLong(0);
    private final AtomicReference<String> lastRequestPath = new AtomicReference<>("");

    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "watch-stream-sse-heartbeat");
        thread.setDaemon(true);
        return thread;
    });

    public WatchStreamServiceImpl(UserDataSourceMapper userDataSourceMapper,
                                  SyncTaskMapper syncTaskMapper,
                                  HealthMetricMapper healthMetricMapper) {
        this.userDataSourceMapper = userDataSourceMapper;
        this.syncTaskMapper = syncTaskMapper;
        this.healthMetricMapper = healthMetricMapper;
    }

    @PostConstruct
    public void startHeartbeat() {
        heartbeatExecutor.scheduleAtFixedRate(this::broadcastPing, 15, 15, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stopHeartbeat() {
        heartbeatExecutor.shutdownNow();
    }

    @Override
    public void markRequest(String path) {
        requestCount.incrementAndGet();
        lastRequestPath.set(path == null ? "" : path);
        lastRequestAt.set(System.currentTimeMillis());
    }

    @Override
    public Map<String, Object> latestResponse() {
        latestCount.incrementAndGet();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", true);
        response.put("data", latestData.get());
        return response;
    }

    @Override
    public Map<String, Object> debugResponse() {
        Map<String, Object> debug = new LinkedHashMap<>();
        debug.put("requestCount", requestCount.get());
        debug.put("ingestCount", ingestCount.get());
        debug.put("latestCount", latestCount.get());
        debug.put("eventsOpenCount", eventsOpenCount.get());
        debug.put("eventsCloseCount", eventsCloseCount.get());
        debug.put("persistSuccessCount", persistSuccessCount.get());
        debug.put("persistFailureCount", persistFailureCount.get());
        debug.put("lastRequestPath", lastRequestPath.get());
        debug.put("lastRequestAt", lastRequestAt.get());
        debug.put("lastIngestAt", lastIngestAt.get());
        debug.put("sseClientCount", sseClients.size());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", true);
        response.put("debug", debug);
        response.put("data", latestData.get());
        return response;
    }

    @Override
    public IngestResult ingest(Map<String, String> queryParams) {
        LinkedHashMap<String, String> rawQuery = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            rawQuery.put(entry.getKey(), entry.getValue());
        }

        Long userId = parseOptionalPositiveLong(queryParams.get("userId"));
        if (userId == null) {
            return IngestResult.error("Invalid userId");
        }

        Long sourceId = parseOptionalPositiveLong(queryParams.get("sourceId"));
        if (queryParams.containsKey("sourceId") && sourceId == null) {
            return IngestResult.error("Invalid sourceId");
        }

        Integer heartRate = parseRequiredNonNegativeInt(queryParams.get("heartRate"));
        if (heartRate == null) {
            return IngestResult.error("Invalid heartRate");
        }

        Integer avgHeartRate = parseOptionalNonNegativeInt(queryParams.get("avgHeartRate"));
        if (queryParams.containsKey("avgHeartRate") && avgHeartRate == null) {
            return IngestResult.error("Invalid avgHeartRate");
        }
        if (avgHeartRate == null) {
            avgHeartRate = heartRate;
        }

        Integer restingHeartRate = parseOptionalNonNegativeInt(queryParams.get("restingHeartRate"));
        if (queryParams.containsKey("restingHeartRate") && restingHeartRate == null) {
            return IngestResult.error("Invalid restingHeartRate");
        }

        Integer maxHeartRate = parseOptionalNonNegativeInt(queryParams.get("maxHeartRate"));
        if (queryParams.containsKey("maxHeartRate") && maxHeartRate == null) {
            return IngestResult.error("Invalid maxHeartRate");
        }
        if (maxHeartRate == null || maxHeartRate < heartRate) {
            maxHeartRate = heartRate;
        }

        Integer stressLevel = parseOptionalNonNegativeInt(queryParams.get("stressLevel"));
        if (queryParams.containsKey("stressLevel") && stressLevel == null) {
            return IngestResult.error("Invalid stressLevel");
        }

        Integer bodyBattery = parseOptionalNonNegativeInt(queryParams.get("bodyBattery"));
        if (queryParams.containsKey("bodyBattery") && bodyBattery == null) {
            return IngestResult.error("Invalid bodyBattery");
        }

        Integer sleepDuration = parseOptionalNonNegativeInt(queryParams.get("sleepDuration"));
        if (queryParams.containsKey("sleepDuration") && sleepDuration == null) {
            return IngestResult.error("Invalid sleepDuration");
        }

        Integer stepsValue = parseOptionalNonNegativeInt(queryParams.get("steps"));
        if (queryParams.containsKey("steps") && stepsValue == null) {
            return IngestResult.error("Invalid steps");
        }

        Integer caloriesValue = parseOptionalNonNegativeInt(queryParams.get("calories"));
        if (queryParams.containsKey("calories") && caloriesValue == null) {
            return IngestResult.error("Invalid calories");
        }

        Integer distanceCmValue = parseOptionalNonNegativeInt(queryParams.get("distanceCm"));
        if (queryParams.containsKey("distanceCm") && distanceCmValue == null) {
            return IngestResult.error("Invalid distanceCm");
        }

        Long timestamp = parseOptionalPositiveLong(queryParams.get("timestamp"));
        if (queryParams.containsKey("timestamp") && timestamp == null) {
            return IngestResult.error("Invalid timestamp");
        }
        if (timestamp == null) {
            timestamp = Instant.now().getEpochSecond();
        }

        String device = normalizeDevice(queryParams.get("device"));
        LocalDate measureDate = resolveMeasureDate(timestamp);
        Double mappedSleepHours = toSleepHours(sleepDuration);
        Integer mappedRestingHeartRate = firstNonNull(restingHeartRate, avgHeartRate, heartRate);

        UserDataSourceDO source;
        try {
            source = resolveDeviceSource(userId, sourceId);
        } catch (IllegalArgumentException ex) {
            persistFailureCount.incrementAndGet();
            return IngestResult.error(ex.getMessage());
        }

        long receivedAt = System.currentTimeMillis();
        SyncTaskDO taskDO = beginSyncTask(userId, source.getSourceId());
        try {
            UpsertOutcome outcome = upsertHealthMetric(userId, source, taskDO.getTaskId(), measureDate,
                    stepsValue, mappedRestingHeartRate, mappedSleepHours, stressLevel);

            finishSuccessTask(taskDO, source, measureDate, outcome);
            WatchStreamData next = new WatchStreamData(
                    userId,
                    source.getSourceId(),
                    taskDO.getTaskId(),
                    heartRate,
                    avgHeartRate,
                    restingHeartRate,
                    maxHeartRate,
                    stressLevel,
                    bodyBattery,
                    sleepDuration,
                    defaultZero(stepsValue),
                    defaultZero(caloriesValue),
                    defaultZero(distanceCmValue),
                    device,
                    timestamp,
                    receivedAt,
                    measureDate,
                    mappedSleepHours,
                    mappedRestingHeartRate,
                    source.getSourceName(),
                    rawQuery
            );
            latestData.set(next);
            ingestCount.incrementAndGet();
            persistSuccessCount.incrementAndGet();
            lastIngestAt.set(receivedAt);
            log.info("[WATCH_INGEST] userId={} sourceId={} date={} steps={} mappedRestingHeartRate={} sleepHours={} stressLevel={} device={}",
                    userId, source.getSourceId(), measureDate, stepsValue, mappedRestingHeartRate, mappedSleepHours, stressLevel, device);
            broadcastUpdate(next);
            return IngestResult.success(next);
        } catch (IllegalArgumentException ex) {
            persistFailureCount.incrementAndGet();
            finishFailureTask(taskDO, source, ex.getMessage());
            return IngestResult.error(ex.getMessage());
        } catch (Exception ex) {
            persistFailureCount.incrementAndGet();
            log.error("Persisting watch stream data failed. userId={}, sourceId={}", userId, source.getSourceId(), ex);
            finishFailureTask(taskDO, source, "保存手表数据失败");
            return IngestResult.error("Failed to persist watch data");
        }
    }

    @Override
    public SseEmitter openEvents() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        sseClients.add(emitter);
        eventsOpenCount.incrementAndGet();

        emitter.onCompletion(() -> removeEmitter(emitter));
        emitter.onTimeout(() -> removeEmitter(emitter));
        emitter.onError(ex -> removeEmitter(emitter));

        WatchStreamData current = latestData.get();
        try {
            emitter.send(SseEmitter.event()
                    .name("watch-stream")
                    .data(current, MediaType.APPLICATION_JSON));
            emitter.send(current, MediaType.APPLICATION_JSON);
        } catch (IOException ex) {
            removeEmitter(emitter);
        }
        return emitter;
    }

    private UserDataSourceDO resolveDeviceSource(Long userId, Long sourceId) {
        if (sourceId != null) {
            UserDataSourceDO source = userDataSourceMapper.selectByIdAndUser(sourceId, userId);
            if (source == null) {
                throw new IllegalArgumentException("Device source not found");
            }
            if (!SOURCE_TYPE_DEVICE.equalsIgnoreCase(String.valueOf(source.getSourceType()))) {
                throw new IllegalArgumentException("sourceId is not a device source");
            }
            if (Integer.valueOf(SOURCE_STATUS_PAUSED).equals(source.getSourceStatus())) {
                throw new IllegalArgumentException("Device source is paused");
            }
            return source;
        }

        List<UserDataSourceDO> deviceSources = userDataSourceMapper.selectByUser(userId).stream()
                .filter(source -> SOURCE_TYPE_DEVICE.equalsIgnoreCase(String.valueOf(source.getSourceType())))
                .filter(source -> Integer.valueOf(SOURCE_STATUS_ACTIVE).equals(source.getSourceStatus()))
                .toList();
        if (deviceSources.isEmpty()) {
            throw new IllegalArgumentException("No active device source found");
        }
        if (deviceSources.size() > 1) {
            throw new IllegalArgumentException("Multiple active device sources found, please pass sourceId");
        }
        return deviceSources.get(0);
    }

    private SyncTaskDO beginSyncTask(Long userId, Long sourceId) {
        SyncTaskDO taskDO = new SyncTaskDO();
        taskDO.setUserId(userId);
        taskDO.setSourceId(sourceId);
        taskDO.setTaskType(TASK_TYPE_SYNC);
        taskDO.setTaskStatus(TASK_STATUS_RUNNING);
        taskDO.setFileName(null);
        taskDO.setMetricCategory(METRIC_CATEGORY_HEALTH);
        taskDO.setTotalCount(0);
        taskDO.setInsertCount(0);
        taskDO.setUpdateCount(0);
        taskDO.setFailCount(0);
        taskDO.setSummaryMessage("设备数据同步中");
        taskDO.setStartedTime(LocalDateTime.now());
        syncTaskMapper.insert(taskDO);
        return taskDO;
    }

    private UpsertOutcome upsertHealthMetric(Long userId,
                                             UserDataSourceDO source,
                                             Long taskId,
                                             LocalDate measureDate,
                                             Integer steps,
                                             Integer mappedRestingHeartRate,
                                             Double mappedSleepHours,
                                             Integer stressLevel) {
        HealthMetricDO existing = healthMetricMapper.selectByUserAndDate(userId, measureDate);
        HealthMetricDO target = new HealthMetricDO();
        target.setUserId(userId);
        target.setMeasureDate(measureDate);
        target.setSteps(mergeInteger(existing == null ? null : existing.getSteps(), steps));
        target.setRestingHeartRate(mergeInteger(existing == null ? null : existing.getRestingHeartRate(), mappedRestingHeartRate));
        target.setSleepHours(mergeDouble(existing == null ? null : existing.getSleepHours(), mappedSleepHours));
        target.setSystolic(existing == null ? null : existing.getSystolic());
        target.setDiastolic(existing == null ? null : existing.getDiastolic());
        target.setStressLevel(mergeInteger(existing == null ? null : existing.getStressLevel(), stressLevel));
        target.setSourceId(source.getSourceId());
        target.setSourceType(source.getSourceType());
        target.setSourceName(source.getSourceName());
        target.setSyncTaskId(taskId);
        if (!hasAnyMetricValue(target)) {
            throw new IllegalArgumentException("No supported watch metrics to persist");
        }

        if (existing == null) {
            healthMetricMapper.insert(target);
            return UpsertOutcome.inserted();
        }
        healthMetricMapper.updateByUserAndDate(target);
        return UpsertOutcome.updated();
    }

    private void finishSuccessTask(SyncTaskDO taskDO, UserDataSourceDO source, LocalDate measureDate, UpsertOutcome outcome) {
        taskDO.setTaskStatus(TASK_STATUS_SUCCESS);
        taskDO.setTotalCount(1);
        taskDO.setInsertCount(outcome.insertCount());
        taskDO.setUpdateCount(outcome.updateCount());
        taskDO.setFailCount(0);
        taskDO.setSummaryMessage("接收手表数据成功，已同步到 " + measureDate);
        taskDO.setFinishedTime(LocalDateTime.now());
        syncTaskMapper.updateSummary(taskDO);
        userDataSourceMapper.updateSyncSnapshot(source.getSourceId(), source.getUserId(), SOURCE_STATUS_ACTIVE, taskDO.getFinishedTime());
    }

    private void finishFailureTask(SyncTaskDO taskDO, UserDataSourceDO source, String message) {
        taskDO.setTaskStatus(TASK_STATUS_FAILED);
        taskDO.setTotalCount(1);
        taskDO.setInsertCount(0);
        taskDO.setUpdateCount(0);
        taskDO.setFailCount(1);
        taskDO.setSummaryMessage(limitSummary("接收手表数据失败：" + message));
        taskDO.setFinishedTime(LocalDateTime.now());
        syncTaskMapper.updateSummary(taskDO);
        userDataSourceMapper.updateSyncSnapshot(source.getSourceId(), source.getUserId(), SOURCE_STATUS_ERROR, source.getLastSyncTime());
    }

    private boolean hasAnyMetricValue(HealthMetricDO metricDO) {
        return metricDO.getSteps() != null
                || metricDO.getRestingHeartRate() != null
                || metricDO.getSleepHours() != null
                || metricDO.getSystolic() != null
                || metricDO.getDiastolic() != null
                || metricDO.getStressLevel() != null;
    }

    private Integer mergeInteger(Integer existingValue, Integer incomingValue) {
        return incomingValue != null ? incomingValue : existingValue;
    }

    private Double mergeDouble(Double existingValue, Double incomingValue) {
        return incomingValue != null ? incomingValue : existingValue;
    }

    private Integer firstNonNull(Integer... values) {
        if (values == null) {
            return null;
        }
        for (Integer value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Double toSleepHours(Integer sleepDuration) {
        if (sleepDuration == null) {
            return null;
        }
        return BigDecimal.valueOf(sleepDuration)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private LocalDate resolveMeasureDate(Long timestamp) {
        if (timestamp == null || timestamp <= 0) {
            return LocalDate.now();
        }
        Instant instant = timestamp >= 10_000_000_000L
                ? Instant.ofEpochMilli(timestamp)
                : Instant.ofEpochSecond(timestamp);
        return instant.atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private Integer parseRequiredNonNegativeInt(String value) {
        if (value == null) {
            return null;
        }
        return parseOptionalNonNegativeInt(value);
    }

    private Integer parseOptionalNonNegativeInt(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            double number = Double.parseDouble(value.trim());
            if (!Double.isFinite(number) || number < 0) {
                return null;
            }
            return (int) Math.floor(number);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long parseOptionalPositiveLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            double number = Double.parseDouble(value.trim());
            if (!Double.isFinite(number) || number <= 0) {
                return null;
            }
            return (long) Math.floor(number);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalizeDevice(String device) {
        String normalized = device == null ? "" : device.trim();
        return StringUtils.hasText(normalized) ? normalized : "Unknown";
    }

    private int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String limitSummary(String summary) {
        if (!StringUtils.hasText(summary)) {
            return summary;
        }
        return summary.length() > 500 ? summary.substring(0, 500) : summary;
    }

    private void broadcastUpdate(WatchStreamData payload) {
        for (SseEmitter emitter : sseClients) {
            try {
                emitter.send(SseEmitter.event()
                        .name("watch-stream")
                        .data(payload, MediaType.APPLICATION_JSON));
                emitter.send(payload, MediaType.APPLICATION_JSON);
            } catch (IOException ex) {
                removeEmitter(emitter);
            }
        }
    }

    private void broadcastPing() {
        for (SseEmitter emitter : sseClients) {
            try {
                emitter.send(SseEmitter.event()
                        .name("ping")
                        .data(System.currentTimeMillis()));
            } catch (IOException ex) {
                removeEmitter(emitter);
            }
        }
    }

    private void removeEmitter(SseEmitter emitter) {
        if (sseClients.remove(emitter)) {
            eventsCloseCount.incrementAndGet();
        }
        try {
            emitter.complete();
        } catch (Exception ignored) {
        }
    }

    private record UpsertOutcome(int insertCount, int updateCount) {
        private static UpsertOutcome inserted() {
            return new UpsertOutcome(1, 0);
        }

        private static UpsertOutcome updated() {
            return new UpsertOutcome(0, 1);
        }
    }
}
