package org.hylee.phms.server.service;

import org.hylee.phms.server.dto.AdminSaveHealthGoalRequest;
import org.hylee.phms.server.vo.AdminHealthGoalVO;

import java.util.List;

public interface AdminHealthGoalService {

    List<AdminHealthGoalVO> listGoals(String keyword, Integer goalStatus);

    AdminHealthGoalVO createGoal(AdminSaveHealthGoalRequest request);

    AdminHealthGoalVO updateGoal(Long goalId, AdminSaveHealthGoalRequest request);
}
