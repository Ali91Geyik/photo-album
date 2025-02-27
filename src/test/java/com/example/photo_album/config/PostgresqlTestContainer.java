// src/test/java/com/example/photo_album/config/PostgresqlTestContainer.java
package com.example.photo_album.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
@Profile("postgres-test") // Only active with postgres-test profile
public class PostgresqlTestContainer {

    private static final String POSTGRES_VERSION = "15-alpine";
    private static PostgreSQLContainer<?> postgres;

    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        if (postgres == null) {
            postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:" + POSTGRES_VERSION))
                    .withDatabaseName("photo_album_test")
                    .withUsername("postgres")
                    .withPassword("789456123")
                    .withReuse(true);
            postgres.start();
        }
        return postgres;
    }
}