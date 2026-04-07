package org.hylee.phms.server.config;

import org.hylee.phms.server.mapper.UserMapper;
import org.hylee.phms.server.persistence.UserDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 默认管理员账号初始化器。
 * <p>
 * 启动时检查是否存在账号 {@code admin}，若不存在则创建默认管理员用户。
 * 该逻辑用于“开箱即用”的开发体验，避免首次启动后无法登录管理端。
 */
@Component
public class AdminAccountInitializer implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountInitializer(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        UserDO admin = userMapper.selectByAccount("admin");
        if (admin != null) {
            return;
        }

        UserDO userDO = new UserDO();
        userDO.setAccount("admin");
        userDO.setPasswordHash(passwordEncoder.encode("123456"));
        userDO.setNickname("管理员");
        userDO.setRealname("系统管理员");
        userDO.setGender(0);
        userDO.setAge(null);
        userDO.setAccountLevel(1);
        userDO.setAccountStatus(0);
        userMapper.insertUser(userDO);
        LOG.info("Initialized default admin account: admin/123456");
    }
}
