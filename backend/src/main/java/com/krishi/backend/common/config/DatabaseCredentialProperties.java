package com.krishi.backend.common.config;

import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "spring.datasource")
public class DatabaseCredentialProperties {

    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /** The constraint reports a boolean result, never the configured password. */
    @AssertTrue(message = "SPRING_DATASOURCE_PASSWORD must be provided")
    public boolean isPasswordConfigured() {
        return password != null && !password.isBlank();
    }
}
