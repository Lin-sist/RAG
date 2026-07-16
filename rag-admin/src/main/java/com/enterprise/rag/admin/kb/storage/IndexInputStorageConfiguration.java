package com.enterprise.rag.admin.kb.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;
import java.util.Arrays;

@Configuration
public class IndexInputStorageConfiguration {

    @Bean
    IndexInputStore indexInputStore(
            @Value("${document.input-storage.root:}") String configuredRoot,
            Environment environment) {
        boolean production = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (production && configuredRoot.isBlank()) {
            throw new IllegalStateException("Production durable index input root is required");
        }

        Path root = configuredRoot.isBlank()
                ? Path.of(System.getProperty("user.dir"), "data", "index-inputs")
                : Path.of(configuredRoot);

        if (production) {
            Path systemTemp = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize();
            if (root.toAbsolutePath().normalize().startsWith(systemTemp)) {
                throw new IllegalStateException("Production durable index input root must not use system temp");
            }
        }
        DataSize maxFileSize = environment.getProperty(
                "document.input-storage.max-file-size", DataSize.class, DataSize.ofMegabytes(50));
        DataSize minimumUsableSpace = environment.getProperty(
                "document.input-storage.minimum-usable-space", DataSize.class, DataSize.ofMegabytes(100));
        return new FileSystemIndexInputStore(
                root, maxFileSize.toBytes(), minimumUsableSpace.toBytes());
    }
}
