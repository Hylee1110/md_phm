package org.hylee.phms.server.service;

import org.hylee.phms.server.dto.SelectHealthGoalRequest;
import org.hylee.phms.server.vo.HealthGoalCardVO;
import org.hylee.phms.server.vo.UserHealthGoalVO;

import java.util.List;

public interface HealthGoalService {

    List<HealthGoalCardVO> listAvailableGoals(String keyword);

    List<UserHealthGoalVO> listUserGoals(Integer userGoalStatus);

    UserHealthGoalVO selectGoal(Long goalId, SelectHealthGoalRequest request);

    void cancelGoal(Long userGoalId);
}
