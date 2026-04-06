package org.hylee.phms.server.mapper;

import org.apache.ibatis.annotations.Param;
import org.hylee.phms.server.persistence.HealthGoalDO;

import java.util.List;

public interface HealthGoalMapper {

    List<HealthGoalDO> selectAdminGoals(@Param("keyword") String keyword, @Param("goalStatus") Integer goalStatus);

    HealthGoalDO selectById(@Param("goalId") Long goalId);

    Long countByCode(@Param("goalCode") String goalCode, @Param("excludeGoalId") Long excludeGoalId);

    int insert(HealthGoalDO healthGoalDO);

    int updateById(HealthGoalDO healthGoalDO);

    List<HealthGoalDO> selectAvailableGoals(@Param("userId") Long userId, @Param("keyword") String keyword);
}
