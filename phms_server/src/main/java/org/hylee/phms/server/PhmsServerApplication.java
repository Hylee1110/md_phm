package org.hylee.phms.server;

import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * 后端服务启动入口。
 * <p>
 * 说明：
 * <ul>
 *   <li>{@link SpringBootApplication}：启用 Spring Boot 自动配置与组件扫描</li>
 *   <li>{@link MapperScan}：扫描 MyBatis Mapper 接口（XML 在 resources/mapper 下）</li>
 * </ul>
 */
@MapperScan("org.hylee.phms.server.mapper")
@SpringBootApplication
public class PhmsServerApplication {
    private static final Logger logger = LoggerFactory.getLogger(PhmsServerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(PhmsServerApplication.class, args);
        logger.info("Phms启动成功.");
    }

}
