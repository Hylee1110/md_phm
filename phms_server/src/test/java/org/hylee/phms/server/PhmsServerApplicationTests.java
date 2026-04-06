package org.hylee.phms.server;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Integration test requires local MySQL and pre-created schema.")
class PhmsServerApplicationTests {

    @Test
    void contextLoads() {
    }
}
