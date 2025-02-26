// src/main/java/com/example/photo_album/config/RetryConfiguration.java
package com.example.photo_album.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry
public class RetryConfiguration {
    // The annotation is enough to enable retry functionality
}