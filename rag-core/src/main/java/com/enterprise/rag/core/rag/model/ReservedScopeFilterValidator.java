package com.enterprise.rag.core.rag.model;

import com.enterprise.rag.common.exception.BusinessException;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Rejects client metadata fields that could claim a server-owned tenant,
 * knowledge-base, or physical collection scope.
 */
public final class ReservedScopeFilterValidator {

    public static final String ERROR_CODE = "RAG_SCOPE_FILTER_RESERVED";

    private static final Set<String> RESERVED_KEYS = Set.of(
            "tenant", "tenantid",
            "kb", "kbid", "knowledgebase", "knowledgebaseid",
            "collection", "collectionname", "vectorcollection", "vectorcollectionname");

    private ReservedScopeFilterValidator() {
    }

    public static Map<String, Object> validateAndCopy(Map<String, Object> filter) {
        if (filter == null || filter.isEmpty()) {
            return Map.of();
        }
        validateMap(filter);
        return Collections.unmodifiableMap(new LinkedHashMap<>(filter));
    }

    private static void validateMap(Map<?, ?> filter) {
        for (Map.Entry<?, ?> entry : filter.entrySet()) {
            if (!(entry.getKey() instanceof String key) || isReserved(key)) {
                throw reservedScopeField();
            }
            validateNested(entry.getValue());
        }
    }

    private static void validateNested(Object value) {
        if (value instanceof Map<?, ?> nestedMap) {
            validateMap(nestedMap);
        } else if (value instanceof Collection<?> collection) {
            collection.forEach(ReservedScopeFilterValidator::validateNested);
        }
    }

    private static boolean isReserved(String key) {
        String lowerCase = key.toLowerCase(Locale.ROOT);
        StringBuilder normalized = new StringBuilder(lowerCase.length());
        lowerCase.codePoints()
                .filter(Character::isLetterOrDigit)
                .forEach(normalized::appendCodePoint);
        return RESERVED_KEYS.contains(normalized.toString());
    }

    private static BusinessException reservedScopeField() {
        return new BusinessException(
                ERROR_CODE,
                "过滤条件包含服务端保留的租户、知识库或集合范围字段");
    }
}
