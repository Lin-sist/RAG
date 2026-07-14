package com.enterprise.rag.auth.config;

import com.enterprise.rag.auth.provider.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class JwtSecretProductionGuardContextTest {

    private static final String KNOWN_DEFAULT =
            "change-me-in-production-please-use-at-least-32-bytes";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(context -> context.getEnvironment().setActiveProfiles("prod"))
            .withUserConfiguration(JwtTestConfiguration.class);

    @Test
    void prodContextFailsBeforeJwtProviderIsAvailableWhenSecretIsKnownDefault() {
        contextRunner
                .withPropertyValues("jwt.secret=" + KNOWN_DEFAULT)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(rootCause(context.getStartupFailure()))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("known-default")
                            .hasMessageNotContaining(KNOWN_DEFAULT);
                });
    }

    @Test
    void prodContextCreatesJwtProviderWhenSecretIsValid() {
        contextRunner
                .withPropertyValues(
                        "jwt.secret=valid-prod-secret-with-at-least-thirty-two-bytes")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(JwtTokenProvider.class);
                });
    }

    private static Throwable rootCause(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JwtProperties.class)
    @Import({JwtSecretProductionGuard.class, JwtTokenProvider.class})
    static class JwtTestConfiguration {
    }
}
