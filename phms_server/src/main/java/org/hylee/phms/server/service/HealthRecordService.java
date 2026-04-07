package org.hylee.phms.server.service;

import org.hylee.phms.server.dto.CreateHealthRecordRequest;
import org.hylee.phms.server.vo.HealthRecordVO;

import java.util.List;

/**
 * 健康打卡记录领域服务。
 * <p>
 * 记录在指定 {@code userGoalId} 下，用于目标维度的连续打卡与趋势。
 */
public interface HealthRecordService {

    List<HealthRecordVO> listRecords(Long userGoalId, Integer rangeDays, Integer limit);

    HealthRecordVO createRecord(Long userGoalId, CreateHealthRecordRequest request);
}
