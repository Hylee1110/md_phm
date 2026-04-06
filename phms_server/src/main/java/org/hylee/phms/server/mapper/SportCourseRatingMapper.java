package org.hylee.phms.server.mapper;

import org.apache.ibatis.annotations.Param;
import org.hylee.phms.server.persistence.SportCourseRatingDO;

public interface SportCourseRatingMapper {

    int upsertRatingLog(@Param("courseId") Long courseId,
                        @Param("userId") Long userId,
                        @Param("score") Integer score,
                        @Param("comment") String comment);

    int refreshCourseRating(@Param("courseId") Long courseId);

    SportCourseRatingDO selectByCourseAndUser(@Param("courseId") Long courseId,
                                              @Param("userId") Long userId);
}
