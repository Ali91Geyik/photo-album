// src/main/java/com/example/photo_album/config/DatabaseConfig.java
package com.example.photo_album.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EntityScan(basePackages = "com.example.photo_album.model")
@EnableJpaRepositories(basePackages = "com.example.photo_album.repository")
public class DatabaseConfig {
    // Configuration class to enable JPA features
    // EntityScan tells Spring where to find our entity classes
    // EnableJpaRepositories tells Spring where to find our repositories
}