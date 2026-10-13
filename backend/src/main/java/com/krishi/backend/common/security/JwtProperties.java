package com.krishi.backend.common.security;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;

    private long expiration;

    /**
     * JJWT selects an HMAC algorithm from the supplied key size. A 32-byte
     * minimum guarantees the minimum key size required for HS256. The
     * assertion reports only a boolean validation result, never the secret.
     */
    @AssertTrue(message = "JWT_SECRET must contain at least 32 UTF-8 bytes")
    public boolean isSecretKeyValid() {
        return secret != null
                && secret.getBytes(StandardCharsets.UTF_8).length >= 32;
    }
}
