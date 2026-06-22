package com.krishi.backend.common.security;

import com.krishi.backend.identity.entity.User;
import com.krishi.backend.identity.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(
            UserRepository userRepository) {

        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(
            String phoneNumber)
            throws UsernameNotFoundException {

        User user = userRepository
                .findByPhoneNumber(phoneNumber)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found"
                        ));


        return new CustomUserDetails(user);
    }
}