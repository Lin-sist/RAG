package com.enterprise.rag.auth.config;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 生产环境 JWT 密钥启动守卫。
 */
@Component
public class JwtSecretProductionGuard {

    private static final String KNOWN_DEFAULT =
            "change-me-in-production-please-use-at-least-32-bytes";
    private static final Pattern UNRESOLVED_PLACEHOLDER =
            Pattern.compile("^\\$\\{[^{}]+}$");

    private final Environment environment;

    public JwtSecretProductionGuard(Environment environment) {
        this.environment = environment;
    }

    public void validate(String secret) {
        if (!environment.acceptsProfiles(Profiles.of("prod"))) {
            return;
        }

        if (secret == null || secret.trim().isEmpty()) {
            throw invalidSecret("blank");
        }

        if (KNOWN_DEFAULT.equals(secret)) {
            throw invalidSecret("known-default");
        }

        if (!secret.equals(secret.trim())) {
            throw invalidSecret("surrounding-whitespace");
        }

        if (UNRESOLVED_PLACEHOLDER.matcher(secret).matches()) {
            throw invalidSecret("unresolved-placeholder");
        }
    }

    private static IllegalStateException invalidSecret(String category) {
        return new IllegalStateException(
                "Invalid jwt.secret configuration (" + category
                        + "). Configure JWT_SECRET for prod.");
    }
}
