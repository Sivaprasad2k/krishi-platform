package com.krishi.backend.identity.service;

import com.krishi.backend.common.security.CustomUserDetails;
import com.krishi.backend.common.security.JwtTokenProvider;
import com.krishi.backend.identity.dto.AuthResponse;
import com.krishi.backend.identity.dto.LoginRequest;
import com.krishi.backend.identity.dto.RegisterRequest;
import com.krishi.backend.identity.entity.Role;
import com.krishi.backend.identity.entity.User;
import com.krishi.backend.identity.mapper.UserMapper;
import com.krishi.backend.identity.dto.UserResponse;
import com.krishi.backend.identity.repository.RoleRepository;
import com.krishi.backend.common.event.DomainEventPublisher;
import com.krishi.backend.identity.event.UserLoggedInEvent;
import com.krishi.backend.identity.event.UserRegisteredEvent;
import com.krishi.backend.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public void register(RegisterRequest request) {

        if (userRepository.existsByPhoneNumber(
                request.getPhoneNumber())) {

            throw new IllegalArgumentException(
                    "Phone number already exists");
        }

        if (request.getEmail() != null &&
                !request.getEmail().isBlank() &&
                userRepository.existsByEmail(
                        request.getEmail())) {

            throw new IllegalArgumentException(
                    "Email already exists");
        }

        Role farmerRole = roleRepository
                .findByName("FARMER")
                .orElseThrow(() ->
                        new IllegalStateException(
                                "FARMER role not found"));

        User user = new User();

        user.setId(UUID.randomUUID());
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setEmail(request.getEmail());

        user.setPasswordHash(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        user.setRole(farmerRole);

        user.setActive(true);

        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);

        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .aggregateId(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .fullName(user.getFullName())
                .occurredAt(LocalDateTime.now())
                .createdBy(user.getId())
                .build();

        domainEventPublisher.publish(event);
    }

    public AuthResponse login(LoginRequest request) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getPhoneNumber(),
                                request.getPassword()
                        )
                );

        CustomUserDetails userDetails =
                (CustomUserDetails)
                        authentication.getPrincipal();

        String token =
                jwtTokenProvider.generateToken(
                        userDetails
                );

        UserLoggedInEvent event = UserLoggedInEvent.builder()
                .aggregateId(userDetails.getUserId())
                .phoneNumber(userDetails.getUsername())
                .occurredAt(LocalDateTime.now())
                .createdBy(userDetails.getUserId())
                .build();

        domainEventPublisher.publish(event);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .build();
    }
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(
            CustomUserDetails userDetails) {

        User user = userRepository.findById(
                        userDetails.getUserId()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        ));

        return userMapper.toResponse(user);
    }
}
