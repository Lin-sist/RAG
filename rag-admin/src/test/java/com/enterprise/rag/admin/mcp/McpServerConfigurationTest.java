package com.enterprise.rag.admin.mcp;

import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStatelessServerTransport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertFalse;

class McpServerConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(McpServerConfiguration.class);

    @Test
    void transportAndCapabilitiesAreAbsentByDefault() {
        contextRunner.run(context -> {
            assertFalse(context.containsBean("mcpServletRegistration"));
            assertFalse(context.getBeansOfType(HttpServletStatelessServerTransport.class).size() > 0);
            assertFalse(context.getBeansOfType(McpStatelessSyncServer.class).size() > 0);
        });
    }
}
