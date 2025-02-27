// src/test/java/com/example/photo_album/controller/PhotoControllerTest.java
package com.example.photo_album.controller;

import com.example.photo_album.config.CITestConfig;
import com.example.photo_album.config.SecurityConfig;
import com.example.photo_album.config.TestConfig;
import com.example.photo_album.model.Photo;
import com.example.photo_album.model.User;
import com.example.photo_album.service.PhotoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PhotoController.class)
@Import({TestConfig.class, SecurityConfig.class, CITestConfig.class})
@ActiveProfiles("ci-test")
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true"
})
class PhotoControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Mock
    private PhotoService photoService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private final String TEST_USER_ID = "user123";
    private final String TEST_USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        PhotoController photoController = new PhotoController(photoService);

        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Create a test user
        testUser = User.builder()
                .id(TEST_USER_ID)
                .username(TEST_USERNAME)
                .email("test@example.com")
                .password("password")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(username = "user123")
    void uploadPhoto_Success() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        Map<String, Float> labels = new HashMap<>();
        labels.put("Person", 99.5f);

        Photo photo = Photo.builder()
                .id("123")
                .fileName("test.jpg")
                .contentType("image/jpeg")
                .size(file.getSize())
                .url("https://test-bucket.s3.amazonaws.com/test.jpg")
                .uploadDate(LocalDateTime.now())
                .labels(labels)
                .user(testUser)
                .build();

        when(photoService.uploadPhotoForUser(eq(TEST_USER_ID), any())).thenReturn(photo);

        // Act & Assert
        mockMvc.perform(multipart("/api/photos")
                        .file(file)
                        .with(user(TEST_USER_ID))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("123"))
                .andExpect(jsonPath("$.fileName").value("test.jpg"))
                .andExpect(jsonPath("$.contentType").value("image/jpeg"))
                .andExpect(jsonPath("$.url").value("https://test-bucket.s3.amazonaws.com/test.jpg"));
    }

    @Test
    @WithMockUser(username = "user123")
    void getAllPhotos_Success() throws Exception {
        // Arrange
        Map<String, Float> labels = new HashMap<>();
        labels.put("Person", 99.5f);

        Photo photo1 = Photo.builder()
                .id("123")
                .fileName("test1.jpg")
                .contentType("image/jpeg")
                .size(1024L)
                .url("https://test-bucket.s3.amazonaws.com/test1.jpg")
                .uploadDate(LocalDateTime.now())
                .labels(labels)
                .user(testUser)
                .build();

        Photo photo2 = Photo.builder()
                .id("456")
                .fileName("test2.jpg")
                .contentType("image/jpeg")
                .size(2048L)
                .url("https://test-bucket.s3.amazonaws.com/test2.jpg")
                .uploadDate(LocalDateTime.now())
                .labels(labels)
                .user(testUser)
                .build();

        List<Photo> photoList = Arrays.asList(photo1, photo2);
        Page<Photo> photoPage = new PageImpl<>(photoList);

        when(photoService.getUserPhotos(eq(TEST_USER_ID), any(Pageable.class))).thenReturn(photoPage);

        // Act & Assert
        mockMvc.perform(get("/api/photos")
                        .with(user(TEST_USER_ID))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value("123"))
                .andExpect(jsonPath("$.content[1].id").value("456"));
    }

    @Test
    @WithMockUser(username = "user123")
    void getPhotoById_Success() throws Exception {
        // Arrange
        String photoId = "123";

        Map<String, Float> labels = new HashMap<>();
        labels.put("Person", 99.5f);

        Photo photo = Photo.builder()
                .id(photoId)
                .fileName("test.jpg")
                .contentType("image/jpeg")
                .size(1024L)
                .url("https://test-bucket.s3.amazonaws.com/test.jpg")
                .uploadDate(LocalDateTime.now())
                .labels(labels)
                .user(testUser)
                .build();

        when(photoService.getUserPhotoById(TEST_USER_ID, photoId)).thenReturn(Optional.of(photo));

        // Act & Assert
        mockMvc.perform(get("/api/photos/{id}", photoId)
                        .with(user(TEST_USER_ID))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(photoId))
                .andExpect(jsonPath("$.fileName").value("test.jpg"));
    }

    @Test
    @WithMockUser(username = "user123")
    void getPhotoById_NotFound() throws Exception {
        // Arrange
        String photoId = "999";
        when(photoService.getUserPhotoById(TEST_USER_ID, photoId)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/photos/{id}", photoId)
                        .with(user(TEST_USER_ID))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user123")
    void addTagToPhoto_Success() throws Exception {
        // Arrange
        String photoId = "123";
        String tag = "vacation";

        Map<String, Float> labels = new HashMap<>();
        labels.put("Person", 99.5f);

        Photo photo = Photo.builder()
                .id(photoId)
                .fileName("test.jpg")
                .contentType("image/jpeg")
                .size(1024L)
                .url("https://test-bucket.s3.amazonaws.com/test.jpg")
                .uploadDate(LocalDateTime.now())
                .tags(Arrays.asList("vacation"))
                .labels(labels)
                .user(testUser)
                .build();

        when(photoService.addTagToUserPhoto(eq(TEST_USER_ID), eq(photoId), eq(tag))).thenReturn(photo);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/photos/{id}/tags", photoId)
                        .with(user(TEST_USER_ID))
                        .param("tag", tag)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(photoId))
                .andExpect(jsonPath("$.tags[0]").value("vacation"));
    }

    @Test
    @WithMockUser(username = "user123")
    void findPhotosByTag_Success() throws Exception {
        // Arrange
        String tag = "vacation";

        Map<String, Float> labels = new HashMap<>();
        labels.put("Person", 99.5f);

        Photo photo1 = Photo.builder()
                .id("123")
                .fileName("vacation1.jpg")
                .tags(Arrays.asList("vacation", "beach"))
                .labels(labels)
                .user(testUser)
                .build();

        Photo photo2 = Photo.builder()
                .id("456")
                .fileName("vacation2.jpg")
                .tags(Arrays.asList("vacation", "mountain"))
                .labels(labels)
                .user(testUser)
                .build();

        List<Photo> photos = Arrays.asList(photo1, photo2);

        when(photoService.findUserPhotosByTag(TEST_USER_ID, tag)).thenReturn(photos);

        // Act & Assert
        mockMvc.perform(get("/api/photos/search/bytag")
                        .with(user(TEST_USER_ID))
                        .param("tag", tag)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("123"))
                .andExpect(jsonPath("$[1].id").value("456"));
    }

    @Test
    @WithMockUser(username = "user123")
    void findPhotosByLabel_Success() throws Exception {
        // Arrange
        String label = "Person";
        Float minConfidence = 75.0f;

        Map<String, Float> labels1 = new HashMap<>();
        labels1.put("Person", 99.5f);

        Map<String, Float> labels2 = new HashMap<>();
        labels2.put("Person", 85.0f);

        Photo photo1 = Photo.builder()
                .id("123")
                .fileName("person1.jpg")
                .labels(labels1)
                .user(testUser)
                .build();

        Photo photo2 = Photo.builder()
                .id("456")
                .fileName("person2.jpg")
                .labels(labels2)
                .user(testUser)
                .build();

        List<Photo> photos = Arrays.asList(photo1, photo2);

        when(photoService.findUserPhotosByLabel(TEST_USER_ID, label, minConfidence)).thenReturn(photos);

        // Act & Assert
        mockMvc.perform(get("/api/photos/search/bylabel")
                        .with(user(TEST_USER_ID))
                        .param("label", label)
                        .param("minConfidence", "75.0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("123"))
                .andExpect(jsonPath("$[1].id").value("456"));
    }

    // Helper method to create a mock Principal
    private Principal createPrincipal(String name) {
        return new Principal() {
            @Override
            public String getName() {
                return name;
            }
        };
    }
}