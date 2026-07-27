package com.enterprise.rag.core.vectorstore.maintenance;

import com.enterprise.rag.core.vectorstore.VectorDocument;

import java.util.List;

/**
 * Read-only capability used exclusively by the disabled-by-default legacy
 * migration workflow. It is intentionally separate from runtime VectorStore.
 */
public interface LegacyVectorSourceReader {

    List<VectorDocument> readByIds(String sourceCollection, List<String> ids);

    int readDimension(String sourceCollection);
}
