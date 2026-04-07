package org.hylee.phms.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 密码编码器配置。
 * <p>
 * 当前实现使用 MD5（用于与既有数据/逻辑兼容）。
 * 注意：MD5 并不适合作为强安全场景下的密码哈希算法；如果未来要提升安全性，
 * 需要配合数据迁移（存量密码重置/升级）再切换到 BCrypt/Argon2 等算法。
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                if (rawPassword == null) {
                    return null;
                }
                byte[] bytes = rawPassword.toString().getBytes(StandardCharsets.UTF_8);
                return DigestUtils.md5DigestAsHex(Objects.requireNonNull(bytes));
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                if (rawPassword == null || encodedPassword == null) {
                    return false;
                }
                return encode(rawPassword).equalsIgnoreCase(encodedPassword);
            }
        };
    }
}
