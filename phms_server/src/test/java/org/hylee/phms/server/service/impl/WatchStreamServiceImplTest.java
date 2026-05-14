package org.hylee.phms.server.service.impl;

import org.hylee.phms.server.mapper.HealthMetricMapper;
import org.hylee.phms.server.mapper.SyncTaskMapper;
import org.hylee.phms.server.mapper.UserDataSourceMapper;
import org.hylee.phms.server.persistence.HealthMetricDO;
import org.hylee.phms.server.persistence.SyncTaskDO;
import org.hylee.phms.server.persistence.UserDataSourceDO;
import org.hylee.phms.server.service.WatchStreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WatchStreamServiceImplTest {

    @Mock
    private UserDataSourceMapper userDataSourceMapper;

    @Mock
    private SyncTaskMapper syncTaskMapper;

    @Mock
    private HealthMetricMapper healthMetricMapper;

    private WatchStreamServiceImpl watchStreamService;

    @BeforeEach
    void setUp() {
        watchStreamService = new WatchStreamServiceImpl(userDataSourceMapper, syncTaskMapper, healthMetricMapper);
    }

    @Test
    void ingestShouldInsertMetricAndCreateSyncTask() {
        LocalDate measureDate = LocalDate.of(2026, 4, 7);
        long timestamp = measureDate.atTime(12, 0).atZone(ZoneId.systemDefault()).toEpochSecond();
        UserDataSourceDO source = buildSource(18L, 7L, "Garmin Watch", 0, "device");
        when(userDataSourceMapper.selectByUser(7L)).thenReturn(List.of(source));
        when(healthMetricMapper.selectByUserAndDate(7L, measureDate)).thenReturn(null);
        doAnswer(invocation -> {
            SyncTaskDO taskDO = invocation.getArgument(0);
            taskDO.setTaskId(901L);
            return 1;
        }).when(syncTaskMapper).insert(any(SyncTaskDO.class));

        WatchStreamService.IngestResult result = watchStreamService.ingest(Map.of(
                "userId", "7",
                "heartRate", "84",
                "avgHeartRate", "68",
                "stressLevel", "41",
                "sleepDuration", "450",
                "steps", "8230",
                "device", "Garmin Watch",
                "timestamp", String.valueOf(timestamp)
        ));

        assertTrue(result.ok());
        assertNotNull(result.data());
        assertEquals(7L, result.data().userId());
        assertEquals(18L, result.data().sourceId());
        assertEquals(measureDate, result.data().measureDate());
        assertEquals(68, result.data().mappedRestingHeartRate());
        assertEquals(7.5d, result.data().mappedSleepHours());

        ArgumentCaptor<HealthMetricDO> metricCaptor = ArgumentCaptor.forClass(HealthMetricDO.class);
        verify(healthMetricMapper).insert(metricCaptor.capture());
        HealthMetricDO inserted = metricCaptor.getValue();
        assertEquals(7L, inserted.getUserId());
        assertEquals(measureDate, inserted.getMeasureDate());
        assertEquals(8230, inserted.getSteps());
        assertEquals(68, inserted.getRestingHeartRate());
        assertEquals(7.5d, inserted.getSleepHours());
        assertEquals(41, inserted.getStressLevel());
        assertEquals(18L, inserted.getSourceId());
        assertEquals("device", inserted.getSourceType());
        assertEquals("Garmin Watch", inserted.getSourceName());
        assertEquals(901L, inserted.getSyncTaskId());

        ArgumentCaptor<SyncTaskDO> taskCaptor = ArgumentCaptor.forClass(SyncTaskDO.class);
        verify(syncTaskMapper).updateSummary(taskCaptor.capture());
        SyncTaskDO finishedTask = taskCaptor.getValue();
        assertEquals(0, finishedTask.getTaskStatus());
        assertEquals(1, finishedTask.getTotalCount());
        assertEquals(1, finishedTask.getInsertCount());
        assertEquals(0, finishedTask.getUpdateCount());
        assertEquals(0, finishedTask.getFailCount());

        verify(userDataSourceMapper).updateSyncSnapshot(eq(18L), eq(7L), eq(0), any(LocalDateTime.class));
    }

    @Test
    void ingestShouldMergeIncomingFieldsWithExistingMetric() {
        LocalDate measureDate = LocalDate.of(2026, 4, 8);
        long timestamp = measureDate.atTime(8, 30).atZone(ZoneId.systemDefault()).toEpochSecond();
        UserDataSourceDO source = buildSource(26L, 9L, "Huawei Watch", 0, "device");
        HealthMetricDO existing = new HealthMetricDO();
        existing.setUserId(9L);
        existing.setMeasureDate(measureDate);
        existing.setSteps(3600);
        existing.setRestingHeartRate(66);
        existing.setSleepHours(6.2d);
        existing.setStressLevel(33);

        when(userDataSourceMapper.selectByUser(9L)).thenReturn(List.of(source));
        when(healthMetricMapper.selectByUserAndDate(9L, measureDate)).thenReturn(existing);
        doAnswer(invocation -> {
            SyncTaskDO taskDO = invocation.getArgument(0);
            taskDO.setTaskId(902L);
            return 1;
        }).when(syncTaskMapper).insert(any(SyncTaskDO.class));

        WatchStreamService.IngestResult result = watchStreamService.ingest(Map.of(
                "userId", "9",
                "heartRate", "92",
                "steps", "9100",
                "sleepDuration", "420",
                "device", "Huawei Watch",
                "timestamp", String.valueOf(timestamp)
        ));

        assertTrue(result.ok());
        ArgumentCaptor<HealthMetricDO> metricCaptor = ArgumentCaptor.forClass(HealthMetricDO.class);
        verify(healthMetricMapper).updateByUserAndDate(metricCaptor.capture());
        HealthMetricDO updated = metricCaptor.getValue();
        assertEquals(9100, updated.getSteps());
        assertEquals(92, updated.getRestingHeartRate());
        assertEquals(7.0d, updated.getSleepHours());
        assertEquals(33, updated.getStressLevel());
        assertEquals(902L, updated.getSyncTaskId());
        assertEquals(26L, updated.getSourceId());

        ArgumentCaptor<SyncTaskDO> taskCaptor = ArgumentCaptor.forClass(SyncTaskDO.class);
        verify(syncTaskMapper).updateSummary(taskCaptor.capture());
        assertEquals(1, taskCaptor.getValue().getUpdateCount());
    }

    @Test
    void ingestShouldRejectWhenNoActiveDeviceSourceExists() {
        UserDataSourceDO fileSource = buildSource(33L, 12L, "CSV Import", 0, "file");
        when(userDataSourceMapper.selectByUser(12L)).thenReturn(List.of(fileSource));

        WatchStreamService.IngestResult result = watchStreamService.ingest(Map.of(
                "userId", "12",
                "heartRate", "78"
        ));

        assertFalse(result.ok());
        assertEquals("No active device source found", result.error());
        verify(syncTaskMapper, never()).insert(any(SyncTaskDO.class));
        verify(healthMetricMapper, never()).insert(any(HealthMetricDO.class));
        verify(healthMetricMapper, never()).updateByUserAndDate(any(HealthMetricDO.class));
    }

    private UserDataSourceDO buildSource(Long sourceId,
                                         Long userId,
                                         String sourceName,
                                         Integer sourceStatus,
                                         String sourceType) {
        UserDataSourceDO sourceDO = new UserDataSourceDO();
        sourceDO.setSourceId(sourceId);
        sourceDO.setUserId(userId);
        sourceDO.setSourceName(sourceName);
        sourceDO.setSourceType(sourceType);
        sourceDO.setSourceStatus(sourceStatus);
        return sourceDO;
    }
}
