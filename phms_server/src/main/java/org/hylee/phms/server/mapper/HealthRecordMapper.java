package org.hylee.phms.server.mapper;

import org.apache.ibatis.annotations.Param;
import org.hylee.phms.server.persistence.HealthRecordDO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 健康打卡记录数据访问（按用户目标与时间范围）。
 */
public interface HealthRecordMapper {

    int insert(HealthRecordDO healthRecordDO);

    HealthRecordDO selectByIdAndUser(@Param("recordId") Long recordId, @Param("userId") Long userId);

    List<HealthRecordDO> selectByUserGoalAndRange(@Param("userGoalId") Long userGoalId,
                                                  @Param("userId") Long userId,
                                                  @Param("goalId") Long goalId,
                                                  @Param("rangeStart") LocalDateTime rangeStart,
                                                  @Param("limit") Integer limit);
}
