// src/test/java/com/example/photo_album/integration/AbstractPostgresqlTest.java
package com.example.photo_album.integration;

import com.example.photo_album.config.PostgresqlTestContainer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for all PostgreSQL integration tests.
 * Tests that extend this class will run against a real PostgreSQL database.
 */
@SpringBootTest
@ActiveProfiles("postgres-test")
@Testcontainers
@Import(PostgresqlTestContainer.class)
public abstract class AbstractPostgresqlTest {

    // Static container shared between all test methods for faster execution
    @Container
    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER = 
            new PostgreSQLContainer<>("postgres:15-alpine")
                .withDatabaseName("photo_album_test")
                .withUsername("postgres")
                .withPassword("789456123");

    static {
        POSTGRES_CONTAINER.withReuse(true);
        POSTGRES_CONTAINER.start();
    }
    
    /**
     * Dynamically set database connection properties from the container.
     */
    @DynamicPropertySource
    static void postgresqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
    }
}