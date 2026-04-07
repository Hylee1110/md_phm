package org.hylee.phms.server.service;

import org.hylee.phms.server.dto.AdminSaveSportCourseRequest;
import org.hylee.phms.server.vo.SportCourseAdminVO;
import org.hylee.phms.server.vo.SportCourseCoverUploadVO;
import org.hylee.phms.server.vo.SportCourseOptionsVO;
import org.hylee.phms.server.vo.SportDictionaryOptionVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 管理端运动课程维护服务。
 * <p>
 * 课程全生命周期、字典选项与封面上传。
 */
public interface AdminSportCourseService {

    List<SportCourseAdminVO> listCourses(String keyword, String status);

    SportCourseAdminVO getCourse(Long courseId);

    SportCourseAdminVO createCourse(AdminSaveSportCourseRequest request);

    SportCourseAdminVO updateCourse(Long courseId, AdminSaveSportCourseRequest request);

    void deleteCourse(Long courseId);

    SportCourseOptionsVO getOptions();

    SportDictionaryOptionVO createAudienceOption(String name);

    SportDictionaryOptionVO createEquipmentOption(String name);

    SportDictionaryOptionVO createBenefitOption(String name);

    SportCourseCoverUploadVO uploadCover(MultipartFile file);
}
