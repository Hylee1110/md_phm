package org.hylee.phms.server.mapper;

import org.apache.ibatis.annotations.Param;
import org.hylee.phms.server.persistence.UserDO;

import java.util.List;

public interface UserMapper {

    UserDO selectById(@Param("userId") Long userId);

    UserDO selectByAccount(@Param("account") String account);

    int insertUser(UserDO userDO);

    List<UserDO> selectNormalUsers(@Param("keyword") String keyword);

    int updateAccountStatus(@Param("userId") Long userId, @Param("accountStatus") Integer accountStatus);

    int updateProfile(UserDO userDO);
}
