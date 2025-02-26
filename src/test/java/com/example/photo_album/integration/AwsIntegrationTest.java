package com.example.photo_album.integration;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.example.photo_album.model.Photo;
import com.example.photo_album.model.User;
import com.example.photo_album.repository.PhotoRepository;
import com.example.photo_album.repository.UserRepository;
import com.example.photo_album.service.PhotoService;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AwsIntegrationTest {

    @Mock
    private AmazonS3 amazonS3;

    @Mock
    private AmazonRekognition rekognition;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PhotoRepository photoRepository;

    private PhotoService photoService;
    private User testUser;
    private MultipartFile testFile;
    private static final String BUCKET_NAME = "test-bucket";

    @BeforeEach
    void setUp() {
        // Initialize PhotoService with mocks
        photoService = new PhotoService(amazonS3, rekognition, photoRepository, userRepository, BUCKET_NAME);

        // Create test user
        testUser = User.builder()
                .id(UUID.randomUUID().toString())
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .createdAt(LocalDateTime.now())
                .build();

        // Create test file
        testFile = new MockMultipartFile(
                "test.jpg",
                "original_test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        // Mock user repository
        when(userRepository.findById(eq(testUser.getId())))
                .thenReturn(Optional.of(testUser));

        // Mock photo repository
        when(photoRepository.save(any(Photo.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testS3Upload() throws IOException {
        // Arrange - Mock S3 responses
        when(amazonS3.putObject(eq(BUCKET_NAME), any(), any(), any()))
                .thenReturn(new PutObjectResult());

        when(amazonS3.getUrl(eq(BUCKET_NAME), any()))
                .thenReturn(new URL("https://test-bucket.s3.amazonaws.com/test-file.jpg"));

        // Mock Rekognition empty response
        DetectLabelsResult emptyLabelsResult = new DetectLabelsResult()
                .withLabels(Arrays.asList());
        when(rekognition.detectLabels(any(DetectLabelsRequest.class)))
                .thenReturn(emptyLabelsResult);

        // Act
        Photo result = photoService.uploadPhotoForUser(testUser.getId(), testFile);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUrl()).contains("https://test-bucket.s3.amazonaws.com/");
        assertThat(result.getContentType()).isEqualTo("image/jpeg");

        verify(amazonS3).putObject(
                eq(BUCKET_NAME),
                any(String.class),
                any(),
                any(ObjectMetadata.class)
        );
        verify(amazonS3).getUrl(eq(BUCKET_NAME), any(String.class));
    }

    @Test
    void testRekognitionAnalysis() throws IOException {
        // Arrange - Mock S3 responses
        when(amazonS3.putObject(eq(BUCKET_NAME), any(), any(), any()))
                .thenReturn(new PutObjectResult());

        when(amazonS3.getUrl(eq(BUCKET_NAME), any()))
                .thenReturn(new URL("https://test-bucket.s3.amazonaws.com/test-file.jpg"));

        // Mock Rekognition response with labels
        DetectLabelsResult labelsResult = new DetectLabelsResult()
                .withLabels(Arrays.asList(
                        new Label().withName("Person").withConfidence(99.8f),
                        new Label().withName("Face").withConfidence(95.0f),
                        new Label().withName("Portrait").withConfidence(94.5f),
                        new Label().withName("Human").withConfidence(99.7f)
                ));
        when(rekognition.detectLabels(any(DetectLabelsRequest.class)))
                .thenReturn(labelsResult);

        // Act
        Photo result = photoService.uploadPhotoForUser(testUser.getId(), testFile);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getLabels()).hasSize(4);
        assertThat(result.getLabels())
                .containsEntry("Person", 99.8f)
                .containsEntry("Face", 95.0f)
                .containsEntry("Portrait", 94.5f)
                .containsEntry("Human", 99.7f);

        // Verify Rekognition was called correctly
        ArgumentCaptor<DetectLabelsRequest> rekognitionCaptor = ArgumentCaptor.forClass(DetectLabelsRequest.class);
        verify(rekognition).detectLabels(rekognitionCaptor.capture());

        DetectLabelsRequest capturedRequest = rekognitionCaptor.getValue();
        assertThat(capturedRequest.getImage().getS3Object().getBucket()).isEqualTo(BUCKET_NAME);
        assertThat(capturedRequest.getMinConfidence()).isEqualTo(75F);
        assertThat(capturedRequest.getMaxLabels()).isEqualTo(10);
    }
}