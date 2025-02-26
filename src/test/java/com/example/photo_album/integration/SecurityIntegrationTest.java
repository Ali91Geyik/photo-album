// src/test/java/com/example/photo_album/integration/SecurityIntegrationTest.java
package com.example.photo_album.integration;

import com.example.photo_album.model.User;
import com.example.photo_album.repository.UserRepository;
import com.example.photo_album.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String TEST_USERNAME = "testuser";
    private final String TEST_PASSWORD = "password123";
    private final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        // Clean up existing test users
        userRepository.findByUsername(TEST_USERNAME)
                .ifPresent(user -> userRepository.delete(user));
        
        userRepository.findByEmail(TEST_EMAIL)
                .ifPresent(user -> userRepository.delete(user));
    }

    @Test
    void testUserRegistration() throws Exception {
        // Test user registration
        mockMvc.perform(post("/api/auth/register")
                .param("username", TEST_USERNAME)
                .param("email", TEST_EMAIL)
                .param("password", TEST_PASSWORD)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.username", is(TEST_USERNAME)))
                .andExpect(jsonPath("$.email", is(TEST_EMAIL)));
    }

    @Test
    void testUserLogin() throws Exception {
        // First register a user
        User user = userService.registerUser(TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD);
        
        // Then test login
        mockMvc.perform(post("/api/auth/login")
                .param("username", TEST_USERNAME)
                .param("password", TEST_PASSWORD)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(user.getId())))
                .andExpect(jsonPath("$.username", is(TEST_USERNAME)))
                .andExpect(jsonPath("$.email", is(TEST_EMAIL)));
    }

    @Test
    void testUnauthenticatedAccess() throws Exception {
        // Test access to protected endpoints without authentication
        mockMvc.perform(get("/api/photos"))
                .andExpect(status().isUnauthorized());
                
        mockMvc.perform(get("/api/albums"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAuthenticationWithBasicAuth() throws Exception {
        // Register a user first
        User user = userService.registerUser(TEST_USERNAME, TEST_EMAIL, TEST_PASSWORD);
        
        // Test access with basic auth
        // Note: We're using the ID as username for Spring Security, as specified in CustomUserDetailsService
        mockMvc.perform(get("/api/photos")
                .header("Authorization", createBasicAuthHeader(user.getId(), TEST_PASSWORD)))
                .andExpect(status().isOk());
    }
    
    // Helper method to create a basic auth header
    private String createBasicAuthHeader(String username, String password) {
        String auth = username + ":" + password;
        return "Basic " + java.util.Base64.getEncoder().encodeToString(auth.getBytes());
    }
}