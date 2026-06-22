package com.krishi.backend.identity.repository;

import com.krishi.backend.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u JOIN FETCH u.role WHERE u.phoneNumber = :phoneNumber")
    Optional<User> findByPhoneNumber(@org.springframework.data.repository.query.Param("phoneNumber") String phoneNumber);

    Optional<User> findByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

}