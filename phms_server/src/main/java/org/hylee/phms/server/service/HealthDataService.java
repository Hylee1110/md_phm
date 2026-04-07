package org.hylee.phms.server.service;

import org.hylee.phms.pojo.model.DashboardSummary;
import org.hylee.phms.pojo.model.HealthMetric;
import org.hylee.phms.pojo.model.UserProfile;
import org.hylee.phms.server.dto.UpdateProfileRequest;
import org.hylee.phms.server.vo.IdCardRecognitionVO;
import org.hylee.phms.server.vo.ProfileDetailVO;

import java.util.List;

/**
 * 健康数据领域服务。
 * <p>
 * 该接口定义了个人档案、健康概览与指标趋势等能力，供控制器层调用。
 * 具体实现通常会组合：
 * <ul>
 *   <li>数据库访问（MyBatis Mapper）</li>
 *   <li>业务规则校验</li>
 *   <li>VO/DO/Model 转换</li>
 * </ul>
 */
public interface HealthDataService {

    /**
     * 获取当前用户的基础档案模型（偏底层，可能用于内部计算/组合）。
     */
    UserProfile getUserProfile();

    /**
     * 获取当前用户的档案详情（面向展示的 VO）。
     */
    ProfileDetailVO getProfileDetail();

    /**
     * 根据身份证号识别基础信息（如姓名、性别、出生日期等，用于辅助录入）。
     *
     * @param idcard 身份证号
     */
    IdCardRecognitionVO recognizeByIdCard(String idcard);

    /**
     * 更新当前用户档案信息，并返回更新后的详情。
     */
    ProfileDetailVO updateProfile(UpdateProfileRequest request);

    /**
     * 获取仪表盘/概览汇总信息。
     */
    DashboardSummary getDashboardSummary();

    /**
     * 获取最近 N 天健康指标列表，用于趋势图展示。
     *
     * @param days 天数
     */
    List<HealthMetric> getRecentMetrics(Integer days);
}
