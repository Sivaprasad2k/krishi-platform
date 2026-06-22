package com.krishi.backend.identity.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record UserResponse(
        UUID id,
        String fullName,
        String phoneNumber,
        String email,
        String role
) {
}