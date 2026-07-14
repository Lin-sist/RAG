package com.enterprise.rag.auth.config;

import com.enterprise.rag.auth.provider.JwtTokenProvider;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;

class JwtSecretProductionGuardTest {

    private static final String KNOWN_DEFAULT =
            "change-me-in-production-please-use-at-least-32-bytes";

    @Test
    void prodKnownDefaultPreventsJwtProviderCreationWithoutLeakingTheValue() {
        JwtProperties properties = jwtProperties(KNOWN_DEFAULT);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        Throwable failure = catchThrowable(() ->
                new JwtTokenProvider(properties, new JwtSecretProductionGuard(environment)));

        assertThat(failure)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.secret")
                .hasMessageContaining("known-default")
                .hasMessageNotContaining(KNOWN_DEFAULT);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void prodBlankSecretPreventsJwtProviderCreation(String secret) {
        JwtProperties properties = jwtProperties(secret);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        Throwable failure = catchThrowable(() ->
                new JwtTokenProvider(properties, new JwtSecretProductionGuard(environment)));

        assertThat(failure)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.secret")
                .hasMessageContaining("blank");
    }

    @Test
    void prodSecretWithSurroundingWhitespaceIsRejectedWithoutNormalization() {
        String secret = " valid-prod-secret-with-at-least-thirty-two-bytes";
        JwtProperties properties = jwtProperties(secret);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        Throwable failure = catchThrowable(() ->
                new JwtTokenProvider(properties, new JwtSecretProductionGuard(environment)));

        assertThat(failure)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("surrounding-whitespace")
                .hasMessageNotContaining(secret);
    }

    @Test
    void prodUnresolvedPlaceholderPreventsJwtProviderCreation() {
        String secret = "${JWT_SECRET}";
        JwtProperties properties = jwtProperties(secret);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        Throwable failure = catchThrowable(() ->
                new JwtTokenProvider(properties, new JwtSecretProductionGuard(environment)));

        assertThat(failure)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unresolved-placeholder")
                .hasMessageNotContaining(secret);
    }

    @Test
    void prodValidSecretAllowsJwtProviderCreation() {
        JwtProperties properties = jwtProperties(
                "valid-prod-secret-with-at-least-thirty-two-bytes");
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThatCode(() ->
                new JwtTokenProvider(properties, new JwtSecretProductionGuard(environment)))
                .doesNotThrowAnyException();
    }

    @Test
    void nonProdKnownDefaultRemainsCompatible() {
        JwtProperties properties = jwtProperties(KNOWN_DEFAULT);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");

        assertThatCode(() ->
                new JwtTokenProvider(properties, new JwtSecretProductionGuard(environment)))
                .doesNotThrowAnyException();
    }

    @Test
    void prodShortSecretIsStillRejectedByJjwt() {
        JwtProperties properties = jwtProperties("too-short");
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        Throwable failure = catchThrowable(() ->
                new JwtTokenProvider(properties, new JwtSecretProductionGuard(environment)));

        assertThat(failure).isInstanceOf(WeakKeyException.class);
    }

    @Test
    void prodMultibyteSecretUsesUtf8BytesForJjwtKeyStrength() {
        String secret = "密钥".repeat(6);
        JwtProperties properties = jwtProperties(secret);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThat(secret.length()).isLessThan(32);
        assertThatCode(() ->
                new JwtTokenProvider(properties, new JwtSecretProductionGuard(environment)))
                .doesNotThrowAnyException();
    }

    private static JwtProperties jwtProperties(String secret) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secret);
        return properties;
    }
}
