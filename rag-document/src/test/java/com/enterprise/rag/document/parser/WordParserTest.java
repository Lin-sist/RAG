package com.enterprise.rag.document.parser;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WordParserTest {

    private final WordParser parser = new WordParser();

    @Test
    void shouldOnlySupportDocxFiles() {
        assertTrue(parser.supports("docx"));
        assertFalse(parser.supports("doc"));
    }

    @Test
    void shouldRejectInvalidDocxContent() {
        ByteArrayInputStream input = new ByteArrayInputStream("not-a-docx".getBytes());

        assertThrows(DocumentParseException.class, () -> parser.parse(input));
    }
}
