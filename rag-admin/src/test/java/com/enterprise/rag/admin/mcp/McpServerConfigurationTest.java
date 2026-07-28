package com.enterprise.rag.admin.mcp;

import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStatelessServerTransport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void resourcePageSizeDefaultsToFiftyAndCannotExceedOneHundred() {
        McpProperties properties = new McpProperties();

        assertEquals(50, properties.getResourcePageSize());
        properties.setResourcePageSize(100);
        assertEquals(100, properties.getResourcePageSize());
        assertThrows(IllegalArgumentException.class,
                () -> properties.setResourcePageSize(101));
    }
}
