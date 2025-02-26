// src/main/java/com/example/photo_album/service/UserService.java
package com.example.photo_album.service;

import com.example.photo_album.model.User;
import com.example.photo_album.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Retryable(value = {Exception.class}, maxAttempts = 5, backoff = @Backoff(delay = 200))
    public User registerUser(String username, String email, String password) {
        log.debug("Attempting to register user with username: {}, email: {}", username, email);

        try {
            // Check if username or email already exists
            if (userRepository.existsByUsername(username)) {
                log.warn("Username already exists: {}", username);
                throw new IllegalArgumentException("Username already exists");
            }

            if (userRepository.existsByEmail(email)) {
                log.warn("Email already exists: {}", email);
                throw new IllegalArgumentException("Email already exists");
            }

            User user = User.builder()
                    .username(username)
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .createdAt(LocalDateTime.now())
                    .build();

            User savedUser = userRepository.save(user);
            log.info("Successfully registered user: {}", username);
            return savedUser;
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation when saving user {}: {}", username, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error when registering user {}: {}", username, e.getMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}