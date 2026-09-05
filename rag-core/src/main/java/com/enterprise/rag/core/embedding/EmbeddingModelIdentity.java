package com.enterprise.rag.core.embedding;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** Immutable identity of one comparable embedding space and request contract. */
public record EmbeddingModelIdentity(
        String providerFamily,
        String model,
        String endpoint,
        String requestContractVersion,
        int dimension) {

    public EmbeddingModelIdentity {
        providerFamily = requireText(providerFamily, "providerFamily");
        model = requireText(model, "model");
        endpoint = requireText(endpoint, "endpoint");
        requestContractVersion = requireText(requestContractVersion, "requestContractVersion");
        if (dimension <= 0) {
            throw new IllegalArgumentException("dimension must be positive");
        }
    }

    public String canonical() {
        return providerFamily + "\n" + model + "\n" + endpoint + "\n"
                + requestContractVersion + "\n" + dimension;
    }

    public String fingerprint() {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return Objects.requireNonNull(value).trim();
    }
}
