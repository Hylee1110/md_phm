package org.hylee.phms.server.service;

import org.hylee.phms.pojo.model.DashboardSummary;
import org.hylee.phms.pojo.model.HealthMetric;
import org.hylee.phms.pojo.model.UserProfile;
import org.hylee.phms.server.dto.UpdateProfileRequest;
import org.hylee.phms.server.vo.IdCardRecognitionVO;
import org.hylee.phms.server.vo.ProfileDetailVO;

import java.util.List;

public interface HealthDataService {

    UserProfile getUserProfile();

    ProfileDetailVO getProfileDetail();

    IdCardRecognitionVO recognizeByIdCard(String idcard);

    ProfileDetailVO updateProfile(UpdateProfileRequest request);

    DashboardSummary getDashboardSummary();

    List<HealthMetric> getRecentMetrics(Integer days);
}
