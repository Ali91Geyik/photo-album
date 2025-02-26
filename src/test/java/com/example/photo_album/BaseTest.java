// src/test/java/com/example/photo_album/BaseTest.java
package com.example.photo_album;

import com.example.photo_album.config.CITestConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base test class that all test classes can extend to ensure consistent test configuration.
 * Automatically imports CITestConfig and sets up appropriate profiles.
 * The actual active profile will be determined by the Maven command line or system properties.
 */
@SpringBootTest
@Import(CITestConfig.class)
@ActiveProfiles({"test", "ci-test"})
public abstract class BaseTest {
    // Common test utilities and setup can go here
}