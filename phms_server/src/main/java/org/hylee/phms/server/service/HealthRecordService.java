package org.hylee.phms.server.service;

import org.hylee.phms.server.dto.CreateHealthRecordRequest;
import org.hylee.phms.server.vo.HealthRecordVO;

import java.util.List;

public interface HealthRecordService {

    List<HealthRecordVO> listRecords(Long userGoalId, Integer rangeDays, Integer limit);

    HealthRecordVO createRecord(Long userGoalId, CreateHealthRecordRequest request);
}
