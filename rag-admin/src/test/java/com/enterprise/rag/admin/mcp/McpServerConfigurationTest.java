package com.enterprise.rag.admin.mcp;

import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStatelessServerTransport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

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

    @Test
    void maxChunkBytesDefaultsToSixtyFourKibAndHasAHardConfigurationCeiling() {
        McpProperties properties = new McpProperties();

        assertEquals(65_536, properties.getMaxChunkBytes());
        properties.setMaxChunkBytes(1_048_576);
        assertEquals(1_048_576, properties.getMaxChunkBytes());
        assertThrows(IllegalArgumentException.class,
                () -> properties.setMaxChunkBytes(1_048_577));
    }

    @Test
    void toolSafetyDefaultsAreClosedAndBounded() {
        McpProperties properties = new McpProperties();

        assertFalse(properties.isExternalToolsEnabled());
        assertFalse(properties.isCacheEnabled());
        assertEquals(131_072, properties.getMaxResultBytes());
        assertEquals(Duration.ofSeconds(5), properties.getReadTimeout());
        assertEquals(Duration.ofSeconds(30), properties.getSearchTimeout());
        assertEquals(Duration.ofSeconds(120), properties.getAskTimeout());
        assertEquals(2, properties.getExpensiveConcurrencyPerUser());
        assertThrows(IllegalArgumentException.class,
                () -> properties.setMaxResultBytes(1_048_577));
        assertThrows(IllegalArgumentException.class,
                () -> properties.setExpensiveConcurrencyPerUser(17));
        assertThrows(IllegalArgumentException.class,
                () -> properties.setReadTimeout(Duration.ZERO));
    }
}
