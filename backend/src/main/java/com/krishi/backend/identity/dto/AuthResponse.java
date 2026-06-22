package com.krishi.backend.identity.dto;

import lombok.Builder;

@Builder
public record AuthResponse(
        String accessToken,
        String tokenType
) {
}