package org.hylee.phms.server.mapper;

import org.apache.ibatis.annotations.Param;
import org.hylee.phms.server.persistence.SportCourseDO;

import java.util.List;

/**
 * 运动课程数据访问（用户端已发布列表、管理端维护与详情查询）。
 */
public interface SportCourseMapper {

    List<SportCourseDO> selectPublishedCourses(@Param("keyword") String keyword,
                                               @Param("limit") Integer limit,
                                               @Param("userId") Long userId);

    List<SportCourseDO> selectAdminCourses(@Param("keyword") String keyword,
                                           @Param("status") String status);

    SportCourseDO selectBaseById(@Param("courseId") Long courseId);

    SportCourseDO selectAdminCourseById(@Param("courseId") Long courseId);

    Long countByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    int insertCourse(SportCourseDO courseDO);

    int updateCourse(SportCourseDO courseDO);

    int softDeleteCourse(@Param("courseId") Long courseId);

    List<Long> selectAudienceIds(@Param("courseId") Long courseId);

    List<Long> selectEquipmentIds(@Param("courseId") Long courseId);

    List<Long> selectBenefitIds(@Param("courseId") Long courseId);

    int deleteCourseAudiences(@Param("courseId") Long courseId);

    int deleteCourseEquipments(@Param("courseId") Long courseId);

    int deleteCourseBenefits(@Param("courseId") Long courseId);

    int insertCourseAudiences(@Param("courseId") Long courseId,
                              @Param("audienceIds") List<Long> audienceIds);

    int insertCourseEquipments(@Param("courseId") Long courseId,
                               @Param("equipmentIds") List<Long> equipmentIds);

    int insertCourseBenefits(@Param("courseId") Long courseId,
                             @Param("benefitIds") List<Long> benefitIds);
}
