package com.enterprise.rag.admin.kb;

import com.enterprise.rag.admin.kb.entity.Document;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class DocumentSerializationTest {

    @Test
    void publicDocumentJsonDoesNotExposeDurableInputFacts() throws Exception {
        Document document = new Document();
        document.setFilePath("objects/secret-storage-key.bin");
        document.setInputSizeBytes(123L);
        document.setInputSha256("secret-sha256-marker");
        document.setInputState("AVAILABLE");

        String json = new ObjectMapper().writeValueAsString(document);

        assertFalse(json.contains("secret-storage-key"));
        assertFalse(json.contains("secret-sha256-marker"));
        assertFalse(json.contains("inputSizeBytes"));
        assertFalse(json.contains("inputState"));
    }
}
