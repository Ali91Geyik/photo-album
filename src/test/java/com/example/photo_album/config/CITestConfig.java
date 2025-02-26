// src/test/java/com/example/photo_album/config/CITestConfig.java
package com.example.photo_album.config;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.s3.AmazonS3;
import com.example.photo_album.service.AlbumService;
import com.example.photo_album.service.PhotoService;
import com.example.photo_album.service.UserService;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

/**
 * Test configuration class that provides mock beans for services and dependencies
 * that might not be available in CI environment.
 * 
 * This configuration is activated only when 'test.with.mocks=true', which should be set
 * in CI environment.
 */
@TestConfiguration
@ConditionalOnProperty(name = "test.with.mocks", havingValue = "true")
public class CITestConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .build();
    }

    @Bean
    @Primary
    public AmazonS3 amazonS3() {
        return Mockito.mock(AmazonS3.class);
    }

    @Bean
    @Primary
    public AmazonRekognition amazonRekognition() {
        return Mockito.mock(AmazonRekognition.class);
    }

    @Bean
    @Primary
    public PhotoService photoService() {
        return Mockito.mock(PhotoService.class);
    }

    @Bean
    @Primary
    public AlbumService albumService() {
        return Mockito.mock(AlbumService.class);
    }

    @Bean
    @Primary
    public UserService userService() {
        return Mockito.mock(UserService.class);
    }
}