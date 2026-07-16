package com.enterprise.rag.admin.kb.storage;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

class IndexInputStorageConfigurationTest {

    @Test
    void productionRequiresAnExplicitDurableRoot() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThrows(IllegalStateException.class,
                () -> new IndexInputStorageConfiguration().indexInputStore("", environment));
    }

    @Test
    void productionRejectsSystemTemporaryDirectory() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        String tempRoot = Path.of(System.getProperty("java.io.tmpdir"), "rag-index-inputs").toString();

        assertThrows(IllegalStateException.class,
                () -> new IndexInputStorageConfiguration().indexInputStore(tempRoot, environment));
    }
}
