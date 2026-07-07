package com.queqiao.sync;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class McpAutoConfigSmokeTest {

    @Autowired
    Environment env;

    @Test
    void mcpServerEnabled() {
        assertThat(env.getProperty("spring.ai.mcp.server.enabled")).isEqualTo("true");
    }
}
