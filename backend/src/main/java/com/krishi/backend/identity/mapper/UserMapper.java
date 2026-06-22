package com.krishi.backend.identity.mapper;

import com.krishi.backend.identity.dto.UserResponse;
import com.krishi.backend.identity.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {

        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .role(user.getRole().getName())
                .build();
    }
}