package org.hylee.phms.server.service;

import org.hylee.phms.pojo.model.MealRecord;
import org.hylee.phms.server.dto.CreateMealRecordRequest;
import org.hylee.phms.server.dto.UpdateMealRecordRequest;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 饮食记录领域服务（当前用户维度）。
 */
public interface MealRecordService {

    MealRecord createRecord(CreateMealRecordRequest request);

    MealRecord updateRecord(Long recordId, UpdateMealRecordRequest request);

    void deleteRecord(Long recordId);

    MealRecord getRecord(Long recordId);

    List<MealRecord> listRecords(LocalDateTime startTime, LocalDateTime endTime, Integer limit);
}
