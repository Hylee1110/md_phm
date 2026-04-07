package org.hylee.phms.server.service;

import org.hylee.phms.pojo.model.ExerciseRecord;
import org.hylee.phms.server.dto.CreateExerciseRecordRequest;
import org.hylee.phms.server.dto.UpdateExerciseRecordRequest;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 运动记录领域服务（当前用户维度）。
 */
public interface ExerciseRecordService {

    ExerciseRecord createRecord(CreateExerciseRecordRequest request);

    ExerciseRecord updateRecord(Long recordId, UpdateExerciseRecordRequest request);

    void deleteRecord(Long recordId);

    ExerciseRecord getRecord(Long recordId);

    /** 按时间范围分页查询；起止时间均可为空。 */
    List<ExerciseRecord> listRecords(LocalDateTime startTime, LocalDateTime endTime, Integer limit);
}
