package com.enterprise.rag.admin.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = McpProtocolMvcTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "rag.mcp.enabled=true",
                "rag.mcp.local-only=true",
                "rag.mcp.external-tools-enabled=true",
                "rag.mcp.max-chunk-bytes=10"
        })
class C15McpConformanceIT {

    private static final String CONFORMANCE_PACKAGE =
            "@modelcontextprotocol/conformance@0.1.15";
    private static final String CONFORMANCE_NODE_PACKAGE = "node@22.17.0";
    private static final List<String> APPLICABLE_SCENARIOS = List.of(
            "server-initialize",
            "ping",
            "tools-list",
            "resources-list",
            "dns-rebinding-protection");
    private static final int CONFORMANCE_PORT = availablePort();

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void configureConformanceEndpoint(DynamicPropertyRegistry registry) {
        registry.add("server.port", () -> CONFORMANCE_PORT);
        registry.add("rag.mcp.allowed-origins",
                () -> "http://127.0.0.1:" + CONFORMANCE_PORT);
    }

    @Test
    void fixedOfficialSuitePassesAllApplicableGenericServerScenarios() throws Exception {
        String serverUrl = "http://127.0.0.1:" + port + "/mcp";

        for (String scenario : APPLICABLE_SCENARIOS) {
            ConformanceResult result = runScenario(serverUrl, scenario);
            assertTrue(result.completed(), () -> scenario + " timed out:\n" + result.output());
            assertEquals(0, result.exitCode(), () -> scenario + " failed:\n" + result.output());
        }
    }

    private ConformanceResult runScenario(String serverUrl, String scenario)
            throws IOException, InterruptedException {
        Path outputDirectory = Path.of("target", "c15-conformance", scenario);
        Files.createDirectories(outputDirectory);
        ProcessBuilder builder = new ProcessBuilder(
                npxExecutable(),
                "-y",
                "-p",
                CONFORMANCE_NODE_PACKAGE,
                "-p",
                CONFORMANCE_PACKAGE,
                "conformance",
                "server",
                "--url",
                serverUrl,
                "--scenario",
                scenario,
                "--spec-version",
                "2025-11-25",
                "--output-dir",
                outputDirectory.toAbsolutePath().toString(),
                "--verbose");
        builder.redirectErrorStream(true);
        builder.environment().put("npm_config_yes", "true");
        Process process = builder.start();
        boolean completed = process.waitFor(Duration.ofSeconds(90).toMillis(), TimeUnit.MILLISECONDS);
        if (!completed) {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = completed ? process.exitValue() : -1;
        return new ConformanceResult(completed, exitCode, output);
    }

    private String npxExecutable() {
        return System.getProperty("os.name", "").toLowerCase().contains("win")
                ? "npx.cmd"
                : "npx";
    }

    private static int availablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to reserve conformance port", exception);
        }
    }

    private record ConformanceResult(boolean completed, int exitCode, String output) {
    }
}
