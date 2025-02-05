// src/test/java/com/example/photo_album/controller/PhotoControllerTest.java
package com.example.photo_album.controller;

import com.example.photo_album.config.TestConfig;
import com.example.photo_album.model.Photo;
import com.example.photo_album.service.PhotoService;
import com.example.photo_album.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PhotoController.class)
@Import({TestConfig.class, SecurityConfig.class})
class PhotoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PhotoService photoService;

    @Test
    void uploadPhoto_Success() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        Photo photo = Photo.builder()
                .id("123")
                .fileName("test.jpg")
                .contentType("image/jpeg")
                .size(file.getSize())
                .url("https://test-bucket.s3.amazonaws.com/test.jpg")
                .build();

        when(photoService.uploadPhoto(any())).thenReturn(photo);

        // Act & Assert
        mockMvc.perform(multipart("/api/photos")
                        .file(file))
                .andExpect(status().isOk());
    }
}