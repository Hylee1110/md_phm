package org.hylee.phms.server.mapper;

import org.apache.ibatis.annotations.Param;
import org.hylee.phms.server.persistence.UserHealthGoalDO;

import java.util.List;

/**
 * 用户与健康目标的关联数据访问（选择、列表、状态更新）。
 */
public interface UserHealthGoalMapper {

    UserHealthGoalDO selectByIdAndUser(@Param("userGoalId") Long userGoalId, @Param("userId") Long userId);

    UserHealthGoalDO selectByUserAndGoal(@Param("userId") Long userId, @Param("goalId") Long goalId);

    List<UserHealthGoalDO> selectByUser(@Param("userId") Long userId, @Param("userGoalStatus") Integer userGoalStatus);

    int insert(UserHealthGoalDO userHealthGoalDO);

    int updateById(UserHealthGoalDO userHealthGoalDO);

    int updateStatusByIdAndUser(@Param("userGoalId") Long userGoalId,
                                @Param("userId") Long userId,
                                @Param("userGoalStatus") Integer userGoalStatus);
}
