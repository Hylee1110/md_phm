package org.hylee.phms.server.service;

import org.hylee.phms.server.dto.CreateUserDataSourceRequest;
import org.hylee.phms.server.dto.UpdateUserDataSourceRequest;
import org.hylee.phms.server.vo.DataSourceOverviewVO;
import org.hylee.phms.server.vo.SyncTaskVO;
import org.hylee.phms.server.vo.UserDataSourceVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户数据源领域服务。
 * <p>
 * 管理多源数据接入配置、文件导入异步任务及数据源概览统计。
 */
public interface DataSourceService {

    List<UserDataSourceVO> listSources();

    UserDataSourceVO createSource(CreateUserDataSourceRequest request);

    UserDataSourceVO updateSource(Long sourceId, UpdateUserDataSourceRequest request);

    UserDataSourceVO updateSourceStatus(Long sourceId, Integer sourceStatus);

    void deleteSource(Long sourceId);

    /** 从上传文件导入健康指标，返回同步任务信息。 */
    SyncTaskVO importHealthMetrics(Long sourceId, MultipartFile file);

    /** 从上传文件导入运动记录，返回同步任务信息。 */
    SyncTaskVO importExerciseRecords(Long sourceId, MultipartFile file);

    /** 查询同步任务；sourceId 为空表示当前用户全部数据源。 */
    List<SyncTaskVO> listSyncTasks(Long sourceId, Integer limit);

    DataSourceOverviewVO getOverview();
}
