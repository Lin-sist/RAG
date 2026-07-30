package com.enterprise.rag.integration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class C15McpProfileContractTest {

    @Test
    void dedicatedProfileRunsOnlyOwnedMcpEvidenceDrivers() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"));

        assertTrue(pom.contains("<id>c15-mcp-readonly</id>"));
        assertTrue(pom.contains("<include>**/C15McpReadOnlyIT.java</include>"));
        assertTrue(pom.contains("<include>**/C15McpConformanceIT.java</include>"));
    }
}
