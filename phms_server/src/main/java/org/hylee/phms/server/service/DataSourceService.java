package org.hylee.phms.server.service;

import org.hylee.phms.server.dto.CreateUserDataSourceRequest;
import org.hylee.phms.server.dto.UpdateUserDataSourceRequest;
import org.hylee.phms.server.vo.DataSourceOverviewVO;
import org.hylee.phms.server.vo.SyncTaskVO;
import org.hylee.phms.server.vo.UserDataSourceVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DataSourceService {

    List<UserDataSourceVO> listSources();

    UserDataSourceVO createSource(CreateUserDataSourceRequest request);

    UserDataSourceVO updateSource(Long sourceId, UpdateUserDataSourceRequest request);

    UserDataSourceVO updateSourceStatus(Long sourceId, Integer sourceStatus);

    void deleteSource(Long sourceId);

    SyncTaskVO importHealthMetrics(Long sourceId, MultipartFile file);

    SyncTaskVO importExerciseRecords(Long sourceId, MultipartFile file);

    List<SyncTaskVO> listSyncTasks(Long sourceId, Integer limit);

    DataSourceOverviewVO getOverview();
}
