package org.hylee.phms.server.mapper;

import org.apache.ibatis.annotations.Param;
import org.hylee.phms.server.persistence.HealthMetricDO;

import java.time.LocalDate;
import java.util.List;

/**
 * 健康指标数据访问（按用户、按测量日期的查询与更新）。
 */
public interface HealthMetricMapper {

    HealthMetricDO selectLatestMetricByUser(@Param("userId") Long userId);

    List<HealthMetricDO> selectRecentMetrics(@Param("userId") Long userId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("limit") Integer limit);

    HealthMetricDO selectByUserAndDate(@Param("userId") Long userId, @Param("measureDate") LocalDate measureDate);

    int insert(HealthMetricDO metricDO);

    int updateByUserAndDate(HealthMetricDO metricDO);

    int clearSourceReferenceByUserAndSource(@Param("userId") Long userId, @Param("sourceId") Long sourceId);

    long countImportedMetricDays(@Param("userId") Long userId);
}
