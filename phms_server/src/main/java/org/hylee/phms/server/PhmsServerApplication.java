package org.hylee.phms.server;

import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@MapperScan("org.hylee.phms.server.mapper")
@SpringBootApplication
public class PhmsServerApplication {
    private static final Logger logger = LoggerFactory.getLogger(PhmsServerApplication.class);

    public static void main(String[] args) {
        
        SpringApplication.run(PhmsServerApplication.class, args);
        logger.info("Phms启动成功.");
    }

}
