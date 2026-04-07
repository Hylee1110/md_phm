package org.hylee.phms.server.mapper;

import org.apache.ibatis.annotations.Param;
import org.hylee.phms.server.persistence.ExerciseRecordDO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 运动记录数据访问（含外部键去重查询）。
 */
public interface ExerciseRecordMapper {

    int insert(ExerciseRecordDO recordDO);

    ExerciseRecordDO selectByIdAndUser(@Param("recordId") Long recordId, @Param("userId") Long userId);

    ExerciseRecordDO selectByUserAndExternalKey(@Param("userId") Long userId,
                                                @Param("dataSource") String dataSource,
                                                @Param("externalId") String externalId);

    List<ExerciseRecordDO> selectByUserAndRange(@Param("userId") Long userId,
                                                @Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime,
                                                @Param("limit") Integer limit);

    long countImportedByUser(@Param("userId") Long userId);

    int updateByIdAndUser(ExerciseRecordDO recordDO);

    int deleteByIdAndUser(@Param("recordId") Long recordId, @Param("userId") Long userId);
}
