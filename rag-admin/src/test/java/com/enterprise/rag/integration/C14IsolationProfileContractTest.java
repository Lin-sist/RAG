package com.enterprise.rag.integration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class C14IsolationProfileContractTest {

    @Test
    void dedicatedProfileRunsOnlyOwnedIsolationEvidenceDrivers() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"));

        assertTrue(pom.contains("<id>c14-isolation-eval</id>"));
        assertTrue(pom.contains("<include>**/C14IsolationAdversarialIT.java</include>"));
        assertTrue(pom.contains("<include>**/MilvusFailureSemanticsIT.java</include>"));
        assertTrue(pom.contains("<include>**/RedisFailureSemanticsIT.java</include>"));
    }
}
