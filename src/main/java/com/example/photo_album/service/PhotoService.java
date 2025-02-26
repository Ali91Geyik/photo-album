// src/main/java/com/example/photo_album/service/PhotoService.java
package com.example.photo_album.service;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.example.photo_album.model.Photo;
import com.example.photo_album.model.User;
import com.example.photo_album.repository.PhotoRepository;
import com.example.photo_album.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PhotoService {
    private final AmazonS3 amazonS3;
    private final String bucketName;
    private final AmazonRekognition rekognition;
    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;

    public PhotoService(
            AmazonS3 amazonS3,
            AmazonRekognition rekognition,
            PhotoRepository photoRepository,
            UserRepository userRepository,
            @Value("${aws.s3.bucket}") String bucketName
    ) {
        this.amazonS3 = amazonS3;
        this.bucketName = bucketName;
        this.rekognition = rekognition;
        this.photoRepository = photoRepository;
        this.userRepository = userRepository;
    }

    /**
     * Upload a photo without user association (for backward compatibility)
     */
    @Transactional
    public Photo uploadPhoto(MultipartFile file) throws IOException {
        String fileName = generateUniqueFileName(file.getOriginalFilename());

        // Upload to S3
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());
        amazonS3.putObject(bucketName, fileName, file.getInputStream(), metadata);

        // Analyze with Rekognition
        Map<String, Float> labels = analyzeImage(fileName);

        // Create and save Photo entity
        Photo photo = Photo.builder()
                .id(UUID.randomUUID().toString())
                .fileName(fileName)
                .contentType(file.getContentType())
                .size(file.getSize())
                .url(generateUrl(fileName))
                .uploadDate(LocalDateTime.now())
                .labels(labels)
                .tags(new ArrayList<>()) // Initialize empty tags list
                .build();

        return photoRepository.save(photo);
    }

    /**
     * Upload a photo associated with a user
     */
    @Transactional
    public Photo uploadPhotoForUser(String userId, MultipartFile file) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        String fileName = generateUniqueFileName(file.getOriginalFilename());

        // Upload to S3
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());
        amazonS3.putObject(bucketName, fileName, file.getInputStream(), metadata);

        // Analyze with Rekognition
        Map<String, Float> labels = analyzeImage(fileName);

        // Create and save Photo entity
        Photo photo = Photo.builder()
                .id(UUID.randomUUID().toString())
                .fileName(fileName)
                .contentType(file.getContentType())
                .size(file.getSize())
                .url(generateUrl(fileName))
                .uploadDate(LocalDateTime.now())
                .labels(labels)
                .tags(new ArrayList<>())
                .user(user)
                .build();

        return photoRepository.save(photo);
    }

    @Transactional(readOnly = true)
    public Page<Photo> getAllPhotos(Pageable pageable) {
        return photoRepository.findAll(pageable);
    }

    /**
     * Get photos for a specific user
     */
    @Transactional(readOnly = true)
    public Page<Photo> getUserPhotos(String userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        return photoRepository.findByUser(user, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Photo> getPhotoById(String id) {
        return photoRepository.findById(id);
    }

    /**
     * Get a photo by ID, checking if it belongs to the specified user
     */
    @Transactional(readOnly = true)
    public Optional<Photo> getUserPhotoById(String userId, String photoId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        Optional<Photo> photoOpt = photoRepository.findById(photoId);

        // Check if photo exists and belongs to the user
        if (photoOpt.isPresent() && photoOpt.get().getUser() != null
                && photoOpt.get().getUser().getId().equals(userId)) {
            return photoOpt;
        }

        return Optional.empty();
    }

    @Transactional
    public Photo addTagToPhoto(String photoId, String tag) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new NoSuchElementException("Photo not found"));

        if (photo.getTags() == null) {
            photo.setTags(new ArrayList<>());
        }
        photo.getTags().add(tag);

        return photoRepository.save(photo);
    }

    /**
     * Add a tag to a photo owned by a specific user
     */
    @Transactional
    public Photo addTagToUserPhoto(String userId, String photoId, String tag) {
        // First verify the photo belongs to the user
        Photo photo = getUserPhotoById(userId, photoId)
                .orElseThrow(() -> new NoSuchElementException("Photo not found or doesn't belong to user"));

        if (photo.getTags() == null) {
            photo.setTags(new ArrayList<>());
        }
        photo.getTags().add(tag);

        return photoRepository.save(photo);
    }

    @Transactional(readOnly = true)
    public List<Photo> findPhotosByTag(String tag) {
        return photoRepository.findByTagsContaining(tag);
    }

    /**
     * Find a user's photos by tag
     */
    @Transactional(readOnly = true)
    public List<Photo> findUserPhotosByTag(String userId, String tag) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        return photoRepository.findByUserAndTagsContaining(user, tag);
    }

    @Transactional(readOnly = true)
    public List<Photo> findPhotosByLabel(String label, Float minConfidence) {
        return photoRepository.findByLabelAndMinConfidence(label, minConfidence);
    }

    /**
     * Find a user's photos by label
     */
    @Transactional(readOnly = true)
    public List<Photo> findUserPhotosByLabel(String userId, String label, Float minConfidence) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        return photoRepository.findByUserAndLabelAndMinConfidence(user, label, minConfidence);
    }

    private Map<String, Float> analyzeImage(String fileName) {
        try {
            DetectLabelsRequest request = new DetectLabelsRequest()
                    .withImage(new Image()
                            .withS3Object(new S3Object()
                                    .withBucket(bucketName)
                                    .withName(fileName)))
                    .withMaxLabels(10)
                    .withMinConfidence(75F);

            DetectLabelsResult result = rekognition.detectLabels(request);

            return result.getLabels().stream()
                    .collect(Collectors.toMap(
                            Label::getName,
                            Label::getConfidence
                    ));

        } catch (AmazonRekognitionException e) {
            log.error("Error during Rekognition analysis", e);
            return new HashMap<>();
        }
    }

    private String generateUniqueFileName(String originalFileName) {
        return UUID.randomUUID().toString() + "_" + originalFileName;
    }

    private String generateUrl(String fileName) {
        return amazonS3.getUrl(bucketName, fileName).toString();
    }
}