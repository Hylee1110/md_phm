package org.hylee.phms.server.mapper;

import org.apache.ibatis.annotations.Param;
import org.hylee.phms.server.persistence.MealRecordDO;

import java.time.LocalDateTime;
import java.util.List;

public interface MealRecordMapper {

    int insert(MealRecordDO mealRecordDO);

    MealRecordDO selectByIdAndUser(@Param("recordId") Long recordId, @Param("userId") Long userId);

    List<MealRecordDO> selectByUserAndRange(@Param("userId") Long userId,
                                            @Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime,
                                            @Param("limit") Integer limit);

    int updateByIdAndUser(MealRecordDO mealRecordDO);

    int softDeleteByIdAndUser(@Param("recordId") Long recordId, @Param("userId") Long userId);
}
