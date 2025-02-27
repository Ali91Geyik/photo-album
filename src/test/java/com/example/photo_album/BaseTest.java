// src/test/java/com/example/photo_album/BaseTest.java
package com.example.photo_album;

import com.example.photo_album.config.CITestConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Base test class that all test classes can extend to ensure consistent test configuration.
 * Automatically imports CITestConfig and sets up appropriate profiles.
 */
@SpringBootTest
@Import(CITestConfig.class)
@ActiveProfiles("ci-test")
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true"
})
public abstract class BaseTest {
    // Common test utilities and setup can go here
}