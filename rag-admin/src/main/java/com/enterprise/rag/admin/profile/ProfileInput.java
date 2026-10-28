package com.enterprise.rag.admin.profile;

import com.enterprise.rag.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;

/** Strict write whitelist, independent of the application's Jackson unknown-field defaults. */
public record ProfileInput(String displayName, String email, int expectedRevision, String avatarAction) {
    public static BusinessException invalid() {
        return new BusinessException("PROFILE_VALIDATION_FAILED", "请检查昵称、邮箱及提交内容");
    }

    public static ProfileInput parse(ObjectMapper mapper, String json) {
        try {
            if (json == null || json.length() > 4096) throw invalid();
            JsonNode root = mapper.reader().with(com.fasterxml.jackson.core.StreamReadFeature.STRICT_DUPLICATE_DETECTION.mappedFeature())
                    .with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readTree(json);
            Set<String> keys = new HashSet<>();
            root.fieldNames().forEachRemaining(keys::add);
            if (!root.isObject() || !keys.equals(Set.of("displayName", "email", "expectedRevision", "avatarAction"))) throw invalid();
            String name = text(root.get("displayName"));
            String email = text(root.get("email"));
            if (name != null && (name.codePointCount(0, name.length()) > 50 || name.codePoints().anyMatch(Character::isISOControl))) throw invalid();
            if (email != null && (email.length() > 100 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))) throw invalid();
            JsonNode revision = root.get("expectedRevision"), action = root.get("avatarAction");
            if (!revision.isIntegralNumber() || !revision.canConvertToInt() || revision.intValue() < 0
                    || !action.isTextual() || !Set.of("KEEP", "REPLACE", "REMOVE").contains(action.textValue())) throw invalid();
            return new ProfileInput(name, email, revision.intValue(), action.textValue());
        } catch (BusinessException e) { throw e; }
        catch (Exception e) { throw invalid(); }
    }

    private static String text(JsonNode node) {
        if (node.isNull()) return null;
        if (!node.isTextual()) throw invalid();
        String value = node.textValue().strip();
        return value.isEmpty() ? null : value;
    }
}
