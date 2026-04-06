package org.hylee.phms.server.service;

import org.hylee.phms.server.dto.RateSportCourseRequest;
import org.hylee.phms.server.vo.SportCourseCardVO;
import org.hylee.phms.server.vo.SportCourseRatingVO;

import java.util.List;

public interface SportCourseService {

    List<SportCourseCardVO> listPublishedCourses(String keyword, Integer limit);

    SportCourseRatingVO rateCourse(Long courseId, RateSportCourseRequest request);
}
