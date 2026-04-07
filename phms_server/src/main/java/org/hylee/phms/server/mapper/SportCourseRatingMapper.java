package org.hylee.phms.server.mapper;

import org.apache.ibatis.annotations.Param;
import org.hylee.phms.server.persistence.SportCourseRatingDO;

/**
 * 运动课程评分流水与课程侧聚合字段刷新。
 */
public interface SportCourseRatingMapper {

    int upsertRatingLog(@Param("courseId") Long courseId,
                        @Param("userId") Long userId,
                        @Param("score") Integer score,
                        @Param("comment") String comment);

    int refreshCourseRating(@Param("courseId") Long courseId);

    SportCourseRatingDO selectByCourseAndUser(@Param("courseId") Long courseId,
                                              @Param("userId") Long userId);
}
