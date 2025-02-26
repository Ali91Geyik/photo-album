// src/test/java/com/example/photo_album/integration/DatabaseIntegrationTest.java
package com.example.photo_album.integration;

import com.example.photo_album.model.Album;
import com.example.photo_album.model.Photo;
import com.example.photo_album.model.User;
import com.example.photo_album.repository.AlbumRepository;
import com.example.photo_album.repository.PhotoRepository;
import com.example.photo_album.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class DatabaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private AlbumRepository albumRepository;

    private User testUser;
    private Photo testPhoto1;
    private Photo testPhoto2;
    private Album testAlbum;

    @BeforeEach
    void setUp() {
        // Clean up from previous tests
        albumRepository.deleteAll();
        photoRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = User.builder()
                .id(UUID.randomUUID().toString())
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .createdAt(LocalDateTime.now())
                .build();
        
        userRepository.save(testUser);

        // Create test photos with tags and labels
        Map<String, Float> labels1 = new HashMap<>();
        labels1.put("Person", 95.0f);
        labels1.put("Outdoors", 85.0f);

        testPhoto1 = Photo.builder()
                .id(UUID.randomUUID().toString())
                .fileName("vacation1.jpg")
                .contentType("image/jpeg")
                .size(1024L)
                .url("https://test-bucket.s3.amazonaws.com/vacation1.jpg")
                .uploadDate(LocalDateTime.now())
                .tags(Arrays.asList("vacation", "beach"))
                .labels(labels1)
                .user(testUser)
                .build();

        Map<String, Float> labels2 = new HashMap<>();
        labels2.put("Person", 92.0f);
        labels2.put("Mountain", 88.0f);

        testPhoto2 = Photo.builder()
                .id(UUID.randomUUID().toString())
                .fileName("vacation2.jpg")
                .contentType("image/jpeg")
                .size(2048L)
                .url("https://test-bucket.s3.amazonaws.com/vacation2.jpg")
                .uploadDate(LocalDateTime.now())
                .tags(Arrays.asList("vacation", "mountain"))
                .labels(labels2)
                .user(testUser)
                .build();

        photoRepository.save(testPhoto1);
        photoRepository.save(testPhoto2);

        // Create test album
        testAlbum = Album.builder()
                .id(UUID.randomUUID().toString())
                .name("Vacation Album")
                .description("Photos from vacation")
                .createdAt(LocalDateTime.now())
                .user(testUser)
                .photos(new ArrayList<>(Arrays.asList(testPhoto1)))
                .build();

        albumRepository.save(testAlbum);
    }

    @Test
    void testUserPhotoRelationship() {
        // Test finding photos by user
        Page<Photo> userPhotos = photoRepository.findByUser(testUser, PageRequest.of(0, 10));
        
        assertThat(userPhotos.getContent()).hasSize(2);
        assertThat(userPhotos.getContent()).extracting(Photo::getId)
                .containsExactlyInAnyOrder(testPhoto1.getId(), testPhoto2.getId());
    }

    @Test
    void testFindPhotosByTag() {
        // Test finding photos by tag
        List<Photo> beachPhotos = photoRepository.findByTagsContaining("beach");
        List<Photo> mountainPhotos = photoRepository.findByTagsContaining("mountain");
        List<Photo> vacationPhotos = photoRepository.findByTagsContaining("vacation");

        assertThat(beachPhotos).hasSize(1);
        assertThat(beachPhotos.get(0).getId()).isEqualTo(testPhoto1.getId());

        assertThat(mountainPhotos).hasSize(1);
        assertThat(mountainPhotos.get(0).getId()).isEqualTo(testPhoto2.getId());

        assertThat(vacationPhotos).hasSize(2);
        assertThat(vacationPhotos).extracting(Photo::getId)
                .containsExactlyInAnyOrder(testPhoto1.getId(), testPhoto2.getId());
    }

    @Test
    void testFindPhotosByUserAndTag() {
        // Test finding photos by user and tag
        List<Photo> userVacationPhotos = photoRepository.findByUserAndTagsContaining(testUser, "vacation");
        
        assertThat(userVacationPhotos).hasSize(2);
        assertThat(userVacationPhotos).extracting(Photo::getId)
                .containsExactlyInAnyOrder(testPhoto1.getId(), testPhoto2.getId());
    }

    @Test
    void testFindPhotosByLabel() {
        // Test finding photos by label with confidence threshold
        List<Photo> personPhotos = photoRepository.findByLabelAndMinConfidence("Person", 90.0f);
        List<Photo> outdoorPhotos = photoRepository.findByLabelAndMinConfidence("Outdoors", 80.0f);

        assertThat(personPhotos).hasSize(2);
        assertThat(personPhotos).extracting(Photo::getId)
                .containsExactlyInAnyOrder(testPhoto1.getId(), testPhoto2.getId());

        assertThat(outdoorPhotos).hasSize(1);
        assertThat(outdoorPhotos.get(0).getId()).isEqualTo(testPhoto1.getId());
    }

    @Test
    void testAlbumPhotoRelationship() {
        // Test finding albums by user
        List<Album> userAlbums = albumRepository.findByUser(testUser);
        
        assertThat(userAlbums).hasSize(1);
        assertThat(userAlbums.get(0).getId()).isEqualTo(testAlbum.getId());
        assertThat(userAlbums.get(0).getPhotos()).hasSize(1);
        assertThat(userAlbums.get(0).getPhotos().get(0).getId()).isEqualTo(testPhoto1.getId());
    }

    @Test
    void testUserRepositoryMethods() {
        // Test user repository methods
        assertThat(userRepository.existsByUsername("testuser")).isTrue();
        assertThat(userRepository.existsByEmail("test@example.com")).isTrue();
        
        Optional<User> foundUser = userRepository.findByUsername("testuser");
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getId()).isEqualTo(testUser.getId());
    }
}