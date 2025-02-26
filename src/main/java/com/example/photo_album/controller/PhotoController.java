// src/main/java/com/example/photo_album/controller/PhotoController.java
package com.example.photo_album.controller;

import com.example.photo_album.model.Photo;
import com.example.photo_album.service.PhotoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/photos")
public class PhotoController {
    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @PostMapping
    public ResponseEntity<Photo> uploadPhoto(
            Principal principal,
            @RequestParam("file") MultipartFile file) {
        try {
            Photo photo = photoService.uploadPhotoForUser(principal.getName(), file);
            return ResponseEntity.ok(photo);
        } catch (IOException e) {
            log.error("Error uploading photo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<Page<Photo>> getAllPhotos(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "uploadDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<Photo> photos = photoService.getUserPhotos(principal.getName(), pageable);

        return ResponseEntity.ok(photos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Photo> getPhotoById(
            Principal principal,
            @PathVariable String id) {
        return photoService.getUserPhotoById(principal.getName(), id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/tags")
    public ResponseEntity<Photo> addTagToPhoto(
            Principal principal,
            @PathVariable String id,
            @RequestParam String tag) {
        try {
            Photo updatedPhoto = photoService.addTagToUserPhoto(principal.getName(), id, tag);
            return ResponseEntity.ok(updatedPhoto);
        } catch (Exception e) {
            log.error("Error adding tag to photo", e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search/bytag")
    public ResponseEntity<List<Photo>> findPhotosByTag(
            Principal principal,
            @RequestParam String tag) {
        List<Photo> photos = photoService.findUserPhotosByTag(principal.getName(), tag);
        return ResponseEntity.ok(photos);
    }

    @GetMapping("/search/bylabel")
    public ResponseEntity<List<Photo>> findPhotosByLabel(
            Principal principal,
            @RequestParam String label,
            @RequestParam(defaultValue = "75.0") Float minConfidence) {

        List<Photo> photos = photoService.findUserPhotosByLabel(principal.getName(), label, minConfidence);
        return ResponseEntity.ok(photos);
    }
}