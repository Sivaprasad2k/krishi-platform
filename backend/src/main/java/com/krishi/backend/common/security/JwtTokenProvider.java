package com.krishi.backend.common.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    private final SecretKey secretKey;

    public JwtTokenProvider(
            JwtProperties jwtProperties) {

        this.jwtProperties = jwtProperties;

        this.secretKey = Keys.hmacShaKeyFor(
                jwtProperties
                        .getSecret()
                        .getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateToken(
            CustomUserDetails userDetails) {

        Date now = new Date();

        Date expiry = new Date(
                now.getTime() +
                        jwtProperties.getExpiration()
        );

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expiry)
                .claim("userId",
                        userDetails.getUserId().toString())
                .claim("role",
                        userDetails
                                .getAuthorities()
                                .iterator()
                                .next()
                                .getAuthority())
                .signWith(secretKey)
                .compact();
    }
    public String extractUsername(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean isValidToken(String token) {

        try {

            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);

            return true;

        } catch (Exception ex) {

            return false;
        }
    }
}