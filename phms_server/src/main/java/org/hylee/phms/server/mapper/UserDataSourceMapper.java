package org.hylee.phms.server.mapper;

import org.apache.ibatis.annotations.Param;
import org.hylee.phms.server.persistence.UserDataSourceDO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户数据源配置数据访问（CRUD、同步快照更新等）。
 */
public interface UserDataSourceMapper {

    int insert(UserDataSourceDO sourceDO);

    UserDataSourceDO selectByIdAndUser(@Param("sourceId") Long sourceId, @Param("userId") Long userId);

    List<UserDataSourceDO> selectByUser(@Param("userId") Long userId);

    int updateByIdAndUser(UserDataSourceDO sourceDO);

    int updateStatusByIdAndUser(@Param("sourceId") Long sourceId,
                                @Param("userId") Long userId,
                                @Param("sourceStatus") Integer sourceStatus);

    int deleteByIdAndUser(@Param("sourceId") Long sourceId, @Param("userId") Long userId);

    int updateSyncSnapshot(@Param("sourceId") Long sourceId,
                           @Param("userId") Long userId,
                           @Param("sourceStatus") Integer sourceStatus,
                           @Param("lastSyncTime") LocalDateTime lastSyncTime);

    long countByUser(@Param("userId") Long userId);

    long countActiveByUser(@Param("userId") Long userId);
}
