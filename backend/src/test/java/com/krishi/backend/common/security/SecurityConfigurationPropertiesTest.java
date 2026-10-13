package com.krishi.backend.common.security;

import com.krishi.backend.common.config.DatabaseCredentialProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;

import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigurationPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withInitializer(context -> {
                context.getEnvironment().getPropertySources()
                        .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
                context.getEnvironment().getPropertySources()
                        .remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
            })
            .withUserConfiguration(SecurityPropertiesConfiguration.class);

    @Test
    void missingJwtSecretFailsWithSafeValidationMessage() {
        String databasePassword = randomValue();
        contextRunner
                .withPropertyValues("jwt.expiration=86400000",
                        "spring.datasource.password=" + databasePassword)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(causeMessages(context.getStartupFailure()))
                            .contains("JWT_SECRET must contain at least 32 UTF-8 bytes")
                            .doesNotContain(databasePassword);
                });
    }

    @Test
    void blankJwtSecretFailsWithSafeValidationMessage() {
        String databasePassword = randomValue();
        contextRunner
                .withPropertyValues("jwt.secret=", "spring.datasource.password=" + databasePassword)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(causeMessages(context.getStartupFailure()))
                            .contains("JWT_SECRET must contain at least 32 UTF-8 bytes")
                            .doesNotContain(databasePassword);
                });
    }

    @Test
    void shortJwtSecretFailsWithoutIncludingSecretValue() {
        String shortSecret = randomValue().substring(0, 12);
        contextRunner
                .withPropertyValues("jwt.secret=" + shortSecret,
                        "spring.datasource.password=" + randomValue())
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(causeMessages(context.getStartupFailure()))
                            .contains("JWT_SECRET must contain at least 32 UTF-8 bytes")
                            .doesNotContain(shortSecret);
                });
    }

    @Test
    void missingDatabasePasswordFailsWithSafeValidationMessage() {
        String jwtSecret = randomSecret();
        contextRunner
                .withPropertyValues("jwt.secret=" + jwtSecret,
                        "spring.datasource.url=jdbc:postgresql://localhost:5432/krishi_test_db")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(causeMessages(context.getStartupFailure()))
                            .contains("SPRING_DATASOURCE_PASSWORD must be provided")
                            .doesNotContain(jwtSecret);
                });
    }

    @Test
    void blankDatabasePasswordFailsWithSafeValidationMessage() {
        String jwtSecret = randomSecret();
        contextRunner
                .withPropertyValues("jwt.secret=" + jwtSecret,
                        "spring.datasource.url=jdbc:postgresql://localhost:5432/krishi_test_db",
                        "spring.datasource.password=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(causeMessages(context.getStartupFailure()))
                            .contains("SPRING_DATASOURCE_PASSWORD must be provided")
                            .doesNotContain(jwtSecret);
                });
    }

    @Test
    void validExternallySuppliedCredentialsLoadSuccessfully() {
        String jwtSecret = randomSecret();
        String databasePassword = randomValue();

        contextRunner
                .withPropertyValues("jwt.secret=" + jwtSecret,
                        "spring.datasource.password=" + databasePassword)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(JwtProperties.class).getSecret()).isEqualTo(jwtSecret);
                    assertThat(context.getBean(DatabaseCredentialProperties.class).getPassword())
                            .isEqualTo(databasePassword);
                });
    }

    private String randomSecret() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private String randomValue() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private String causeMessages(Throwable failure) {
        StringBuilder messages = new StringBuilder();
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null) {
                messages.append(cause.getMessage()).append('\n');
            }
        }
        return messages.toString();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({JwtProperties.class, DatabaseCredentialProperties.class})
    static class SecurityPropertiesConfiguration {
    }
}
