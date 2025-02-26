// src/main/java/com/example/photo_album/repository/PhotoRepository.java
package com.example.photo_album.repository;

import com.example.photo_album.model.Photo;
import com.example.photo_album.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhotoRepository extends JpaRepository<Photo, String> {
    // Find photos by tag
    List<Photo> findByTagsContaining(String tag);

    // Find photos by user
    Page<Photo> findByUser(User user, Pageable pageable);

    // Find user's photos by tag
    List<Photo> findByUserAndTagsContaining(User user, String tag);

    // Find photos by label with confidence above threshold
    @Query("SELECT p FROM Photo p JOIN p.labels l WHERE KEY(l) = :labelName AND VALUE(l) >= :minConfidence")
    List<Photo> findByLabelAndMinConfidence(String labelName, Float minConfidence);

    // Find user's photos by label with confidence above threshold
    @Query("SELECT p FROM Photo p JOIN p.labels l WHERE p.user = :user AND KEY(l) = :labelName AND VALUE(l) >= :minConfidence")
    List<Photo> findByUserAndLabelAndMinConfidence(User user, String labelName, Float minConfidence);

    // Find photos by content type
    List<Photo> findByContentType(String contentType);
}