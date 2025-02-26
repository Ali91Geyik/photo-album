// src/test/java/com/example/photo_album/service/PhotoServiceTest.java
package com.example.photo_album.service;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.example.photo_album.model.Photo;
import com.example.photo_album.model.User;
import com.example.photo_album.repository.PhotoRepository;
import com.example.photo_album.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhotoServiceTest {

    @Mock
    private AmazonS3 amazonS3;

    @Mock
    private AmazonRekognition rekognition;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private UserRepository userRepository;

    private PhotoService photoService;
    private static final String BUCKET_NAME = "test-bucket";
    private User testUser;

    @BeforeEach
    void setUp() {
        // Create a test user
        testUser = User.builder()
                .id(UUID.randomUUID().toString())
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .createdAt(LocalDateTime.now())
                .build();

        // Fixed constructor to match actual implementation
        photoService = new PhotoService(amazonS3, rekognition, photoRepository, userRepository, BUCKET_NAME);
    }

    @Test
    void uploadPhotoForUser_Success() throws IOException {
        // Arrange
        MultipartFile file = new MockMultipartFile(
                "test.jpg",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        // Mock UserRepository response
        when(userRepository.findById(eq(testUser.getId())))
                .thenReturn(Optional.of(testUser));

        // Mock S3 responses
        when(amazonS3.putObject(eq(BUCKET_NAME), any(), any(), any()))
                .thenReturn(new PutObjectResult());

        when(amazonS3.getUrl(eq(BUCKET_NAME), any()))
                .thenReturn(new URL("https://test-bucket.s3.amazonaws.com/test.jpg"));

        // Mock Rekognition response
        DetectLabelsResult labelsResult = new DetectLabelsResult()
                .withLabels(Arrays.asList(
                        new Label().withName("Person").withConfidence(99.8f),
                        new Label().withName("Face").withConfidence(95.0f)
                ));
        when(rekognition.detectLabels(any(DetectLabelsRequest.class)))
                .thenReturn(labelsResult);

        // Mock repository response
        when(photoRepository.save(any(Photo.class))).thenAnswer(invocation -> {
            Photo photo = invocation.getArgument(0);
            return photo; // Return the same photo that was passed to save()
        });

        // Act
        Photo result = photoService.uploadPhotoForUser(testUser.getId(), file);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFileName()).contains("test.jpg");
        assertThat(result.getContentType()).isEqualTo("image/jpeg");
        assertThat(result.getSize()).isEqualTo(file.getSize());
        assertThat(result.getUrl()).contains("test-bucket.s3.amazonaws.com");
        assertThat(result.getLabels())
                .containsEntry("Person", 99.8f)
                .containsEntry("Face", 95.0f);
        assertThat(result.getUser()).isEqualTo(testUser);

        // Verify repository was called
        ArgumentCaptor<Photo> photoCaptor = ArgumentCaptor.forClass(Photo.class);
        verify(photoRepository).save(photoCaptor.capture());
        Photo savedPhoto = photoCaptor.getValue();
        assertThat(savedPhoto.getContentType()).isEqualTo("image/jpeg");
        assertThat(savedPhoto.getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    void getUserPhotoById_Success() {
        // Arrange
        String photoId = "test-id";
        String userId = testUser.getId();

        Photo mockPhoto = Photo.builder()
                .id(photoId)
                .fileName("test.jpg")
                .contentType("image/jpeg")
                .size(1000L)
                .url("https://test-bucket.s3.amazonaws.com/test.jpg")
                .uploadDate(LocalDateTime.now())
                .user(testUser)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(photoRepository.findById(photoId)).thenReturn(Optional.of(mockPhoto));

        // Act
        Optional<Photo> result = photoService.getUserPhotoById(userId, photoId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(photoId);
        assertThat(result.get().getUser().getId()).isEqualTo(userId);
        verify(photoRepository).findById(photoId);
        verify(userRepository).findById(userId);
    }

    @Test
    void findUserPhotosByTag_Success() {
        // Arrange
        String tag = "vacation";

        Photo photo1 = Photo.builder()
                .id("1")
                .tags(Arrays.asList("vacation", "beach"))
                .user(testUser)
                .build();

        Photo photo2 = Photo.builder()
                .id("2")
                .tags(Arrays.asList("vacation", "mountain"))
                .user(testUser)
                .build();

        List<Photo> mockPhotos = Arrays.asList(photo1, photo2);

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(photoRepository.findByUserAndTagsContaining(eq(testUser), eq(tag))).thenReturn(mockPhotos);

        // Act
        List<Photo> result = photoService.findUserPhotosByTag(testUser.getId(), tag);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo("1");
        assertThat(result.get(1).getId()).isEqualTo("2");
        verify(photoRepository).findByUserAndTagsContaining(testUser, tag);
    }

    @Test
    void findUserPhotosByLabel_Success() {
        // Arrange
        String label = "Person";
        Float minConfidence = 90.0f;

        Photo photo1 = Photo.builder()
                .id("1")
                .user(testUser)
                .build();

        Photo photo2 = Photo.builder()
                .id("2")
                .user(testUser)
                .build();

        List<Photo> mockPhotos = Arrays.asList(photo1, photo2);

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(photoRepository.findByUserAndLabelAndMinConfidence(eq(testUser), eq(label), eq(minConfidence)))
                .thenReturn(mockPhotos);

        // Act
        List<Photo> result = photoService.findUserPhotosByLabel(testUser.getId(), label, minConfidence);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo("1");
        assertThat(result.get(1).getId()).isEqualTo("2");
        verify(photoRepository).findByUserAndLabelAndMinConfidence(testUser, label, minConfidence);
    }
}