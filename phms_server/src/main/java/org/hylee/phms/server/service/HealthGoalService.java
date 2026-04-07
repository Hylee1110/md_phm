package org.hylee.phms.server.service;

import org.hylee.phms.server.dto.SelectHealthGoalRequest;
import org.hylee.phms.server.vo.HealthGoalCardVO;
import org.hylee.phms.server.vo.UserHealthGoalVO;

import java.util.List;

/**
 * 健康目标领域服务（普通用户侧）。
 * <p>
 * 可选目标浏览、用户选择与取消，与打卡记录通过 userGoalId 关联。
 */
public interface HealthGoalService {

    List<HealthGoalCardVO> listAvailableGoals(String keyword);

    List<UserHealthGoalVO> listUserGoals(Integer userGoalStatus);

    UserHealthGoalVO selectGoal(Long goalId, SelectHealthGoalRequest request);

    void cancelGoal(Long userGoalId);
}
