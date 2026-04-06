package org.hylee.phms.server.service;

import org.hylee.phms.pojo.model.ExerciseRecord;
import org.hylee.phms.server.dto.CreateExerciseRecordRequest;
import org.hylee.phms.server.dto.UpdateExerciseRecordRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface ExerciseRecordService {

    ExerciseRecord createRecord(CreateExerciseRecordRequest request);

    ExerciseRecord updateRecord(Long recordId, UpdateExerciseRecordRequest request);

    void deleteRecord(Long recordId);

    ExerciseRecord getRecord(Long recordId);

    List<ExerciseRecord> listRecords(LocalDateTime startTime, LocalDateTime endTime, Integer limit);
}
