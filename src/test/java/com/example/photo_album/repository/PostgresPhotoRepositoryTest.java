// src/test/java/com/example/photo_album/repository/PostgresPhotoRepositoryTest.java
package com.example.photo_album.repository;

import com.example.photo_album.integration.AbstractPostgresqlTest;
import com.example.photo_album.model.Photo;
import com.example.photo_album.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL-specific tests for PhotoRepository.
 * Tests features that might behave differently between H2 and PostgreSQL.
 */
class PostgresPhotoRepositoryTest extends AbstractPostgresqlTest {

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Photo photo1;
    private Photo photo2;
    private Photo photo3;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up from previous tests
        photoRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user WITHOUT specifying ID (let Hibernate generate it)
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .createdAt(LocalDateTime.now())
                .build();

        testUser = userRepository.save(testUser);  // Save and get the generated ID

        // Create test photos with various labels and confidences
        Map<String, Float> labels1 = new HashMap<>();
        labels1.put("Person", 95.5f);
        labels1.put("Outdoors", 87.3f);
        labels1.put("Nature", 82.1f);

        photo1 = Photo.builder()
                .id(UUID.randomUUID().toString())
                .fileName("test1.jpg")
                .contentType("image/jpeg")
                .size(1024L)
                .url("https://test-bucket.s3.amazonaws.com/test1.jpg")
                .uploadDate(LocalDateTime.now().minusDays(2))
                .tags(Arrays.asList("vacation", "family"))
                .labels(labels1)
                .user(testUser)
                .build();

        Map<String, Float> labels2 = new HashMap<>();
        labels2.put("Person", 92.7f);
        labels2.put("Building", 88.4f);
        labels2.put("City", 85.9f);

        photo2 = Photo.builder()
                .id(UUID.randomUUID().toString())
                .fileName("test2.jpg")
                .contentType("image/jpeg")
                .size(2048L)
                .url("https://test-bucket.s3.amazonaws.com/test2.jpg")
                .uploadDate(LocalDateTime.now().minusDays(1))
                .tags(Arrays.asList("city", "travel"))
                .labels(labels2)
                .user(testUser)
                .build();

        Map<String, Float> labels3 = new HashMap<>();
        labels3.put("Person", 89.2f); // Below 90 threshold for some tests
        labels3.put("Food", 96.1f);
        labels3.put("Restaurant", 91.3f);

        photo3 = Photo.builder()
                .id(UUID.randomUUID().toString())
                .fileName("test3.jpg")
                .contentType("image/jpeg")
                .size(3072L)
                .url("https://test-bucket.s3.amazonaws.com/test3.jpg")
                .uploadDate(LocalDateTime.now())
                .tags(Arrays.asList("food", "restaurant"))
                .labels(labels3)
                .user(testUser)
                .build();

        photoRepository.saveAll(Arrays.asList(photo1, photo2, photo3));
    }

    @Test
    @Transactional
    void testFindByLabelAndMinConfidence() {
        // Test query with different confidence thresholds
        List<Photo> highConfidence = photoRepository.findByLabelAndMinConfidence("Person", 90.0f);
        assertThat(highConfidence).hasSize(2);
        assertThat(highConfidence).extracting(Photo::getId)
                .containsExactlyInAnyOrder(photo1.getId(), photo2.getId());

        List<Photo> mediumConfidence = photoRepository.findByLabelAndMinConfidence("Person", 85.0f);
        assertThat(mediumConfidence).hasSize(3);

        // Test non-existent label
        List<Photo> noResults = photoRepository.findByLabelAndMinConfidence("Car", 80.0f);
        assertThat(noResults).isEmpty();
    }

    @Test
    @Transactional
    void testFindByUserAndLabelAndMinConfidence() {
        // Test with user and label
        List<Photo> results = photoRepository.findByUserAndLabelAndMinConfidence(testUser, "Food", 90.0f);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(photo3.getId());
    }

    @Test
    @Transactional
    void testPaginationAndSorting() {
        // Test pagination with PostgreSQL
        Page<Photo> page1 = photoRepository.findByUser(
                testUser, 
                PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "uploadDate"))
        );
        
        assertThat(page1.getTotalElements()).isEqualTo(3);
        assertThat(page1.getContent()).hasSize(2);
        assertThat(page1.getContent().get(0).getId()).isEqualTo(photo3.getId()); // Most recent
        
        // Get second page
        Page<Photo> page2 = photoRepository.findByUser(
                testUser, 
                PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "uploadDate"))
        );
        
        assertThat(page2.getContent()).hasSize(1);
        assertThat(page2.getContent().get(0).getId()).isEqualTo(photo1.getId()); // Oldest
    }

    @Test
    @Transactional
    void testMapQueryPerformance() {
        // Create many photos with various labels to test query performance
        List<Photo> bulkPhotos = new ArrayList<>();
        
        for (int i = 0; i < 50; i++) {
            Map<String, Float> labels = new HashMap<>();
            labels.put("Person", 90.0f + (i % 10));
            labels.put("Outdoors", 80.0f + (i % 15));
            labels.put("Building", 70.0f + (i % 20));
            
            Photo photo = Photo.builder()
                    .id(UUID.randomUUID().toString())
                    .fileName("bulk" + i + ".jpg")
                    .contentType("image/jpeg")
                    .size(1000L + i)
                    .url("https://test-bucket.s3.amazonaws.com/bulk" + i + ".jpg")
                    .uploadDate(LocalDateTime.now().minusHours(i))
                    .tags(Arrays.asList("tag" + (i % 5)))
                    .labels(labels)
                    .user(testUser)
                    .build();
            
            bulkPhotos.add(photo);
        }
        
        photoRepository.saveAll(bulkPhotos);
        
        // Test query performance for different labels
        List<Photo> personPhotos = photoRepository.findByLabelAndMinConfidence("Person", 95.0f);
        List<Photo> outdoorPhotos = photoRepository.findByLabelAndMinConfidence("Outdoors", 85.0f);
        
        // Just assert that queries execute successfully and return results
        assertThat(personPhotos).isNotEmpty();
        assertThat(outdoorPhotos).isNotEmpty();
    }

    @Test
    @Transactional
    void testComplexTagQueries() {
        // Add a photo with multiple tags that overlap with existing photos
        Map<String, Float> labels = new HashMap<>();
        labels.put("Food", 92.5f);
        
        Photo photo4 = Photo.builder()
                .id(UUID.randomUUID().toString())
                .fileName("test4.jpg")
                .contentType("image/jpeg")
                .size(1500L)
                .url("https://test-bucket.s3.amazonaws.com/test4.jpg")
                .uploadDate(LocalDateTime.now())
                .tags(Arrays.asList("food", "family", "vacation"))
                .labels(labels)
                .user(testUser)
                .build();
        
        photoRepository.save(photo4);
        
        // Test finding by multiple tags
        List<Photo> foodPhotos = photoRepository.findByTagsContaining("food");
        assertThat(foodPhotos).hasSize(2);
        
        List<Photo> vacationPhotos = photoRepository.findByTagsContaining("vacation");
        assertThat(vacationPhotos).hasSize(2);
        
        List<Photo> familyPhotos = photoRepository.findByTagsContaining("family");
        assertThat(familyPhotos).hasSize(2);
    }
}