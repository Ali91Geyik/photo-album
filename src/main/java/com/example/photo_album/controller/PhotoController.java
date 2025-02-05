package com.example.photo_album.controller;

import com.example.photo_album.model.Photo;
import com.example.photo_album.service.PhotoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/photos")
public class PhotoController {
    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @PostMapping
    public ResponseEntity<Photo> uploadPhoto(@RequestParam("file") MultipartFile file) {
        try {
            Photo photo = photoService.uploadPhoto(file);
            return ResponseEntity.ok(photo);
        } catch (IOException e) {
            log.error("Error uploading photo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}