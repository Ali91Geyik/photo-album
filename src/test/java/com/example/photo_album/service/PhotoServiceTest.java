package com.example.photo_album.service;


import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.example.photo_album.model.Photo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhotoServiceTest {

    @Mock
    private AmazonS3 amazonS3;

    private PhotoService photoService;
    private static final String BUCKET_NAME = "test-bucket";

    @BeforeEach
    void setUp() {
        photoService = new PhotoService(amazonS3, BUCKET_NAME);
    }

    @Test
    void uploadPhoto_Success() throws IOException {
        // Arrange
        MultipartFile file = new MockMultipartFile(
                "test.jpg",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        when(amazonS3.putObject(eq(BUCKET_NAME), any(), any(), any()))
                .thenReturn(new PutObjectResult());

        when(amazonS3.getUrl(eq(BUCKET_NAME), any()))
                .thenReturn(new URL("https://test-bucket.s3.amazonaws.com/test.jpg"));

        // Act
        Photo result = photoService.uploadPhoto(file);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFileName()).contains("test.jpg");
        assertThat(result.getContentType()).isEqualTo("image/jpeg");
        assertThat(result.getSize()).isEqualTo(file.getSize());
        assertThat(result.getUrl()).contains("test-bucket.s3.amazonaws.com");
    }
}
