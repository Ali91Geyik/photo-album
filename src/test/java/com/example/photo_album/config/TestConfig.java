// src/test/java/com/example/photo_album/config/TestConfig.java
package com.example.photo_album.config;

import com.example.photo_album.service.PhotoService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.mockito.Mockito;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public PhotoService photoService() {
        return Mockito.mock(PhotoService.class);
    }
}