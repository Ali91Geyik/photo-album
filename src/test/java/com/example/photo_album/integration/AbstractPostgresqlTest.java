// src/test/java/com/example/photo_album/integration/AbstractPostgresqlTest.java
package com.example.photo_album.integration;

import com.example.photo_album.config.CITestConfig;
import com.example.photo_album.config.PostgresqlTestContainer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for all PostgreSQL integration tests.
 * Tests that extend this class will run against a real PostgreSQL database when run locally,
 * but will use mocks when run in CI environment.
 */
@SpringBootTest
@ActiveProfiles({"ci-test"}) // Using only ci-test profile for simplicity
@Testcontainers
@Import({CITestConfig.class})
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true"
})
public abstract class AbstractPostgresqlTest {

    // Static container shared between all test methods for faster execution
    // This will only be used when not running in the CI environment
    @Container
    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("photo_album_test")
                    .withUsername("postgres")
                    .withPassword("789456123");

    static {
        // Only start the container when running locally (not in CI)
        POSTGRES_CONTAINER.withReuse(true);

        // Only attempt to start if we're not in CI mode
        if (!Boolean.getBoolean("CI")) {
            try {
                POSTGRES_CONTAINER.start();
            } catch (Exception e) {
                // If container can't start (e.g., in CI), we'll fall back to H2
                System.err.println("PostgreSQL container could not start, will use H2: " + e.getMessage());
            }
        }
    }

    /**
     * Dynamically set database connection properties from the container.
     * These properties will only be set when not running in CI environment.
     */
    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        if (POSTGRES_CONTAINER.isRunning()) {
            registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
            registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        }
    }
}