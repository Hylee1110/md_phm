package org.hylee.phms.server.mapper;

import org.apache.ibatis.annotations.Param;
import org.hylee.phms.server.persistence.SyncTaskDO;

import java.util.List;

/**
 * 数据导入/同步任务记录数据访问。
 */
public interface SyncTaskMapper {

    int insert(SyncTaskDO taskDO);

    int updateSummary(SyncTaskDO taskDO);

    List<SyncTaskDO> selectRecentByUser(@Param("userId") Long userId,
                                        @Param("sourceId") Long sourceId,
                                        @Param("limit") Integer limit);

    SyncTaskDO selectLatestByUser(@Param("userId") Long userId);

    long countByUser(@Param("userId") Long userId);

    int deleteBySourceIdAndUser(@Param("sourceId") Long sourceId, @Param("userId") Long userId);
}
