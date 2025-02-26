// src/main/java/com/example/photo_album/controller/AlbumController.java
package com.example.photo_album.controller;

import com.example.photo_album.model.Album;
import com.example.photo_album.service.AlbumService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/albums")
public class AlbumController {
    private final AlbumService albumService;

    public AlbumController(AlbumService albumService) {
        this.albumService = albumService;
    }

    @PostMapping
    public ResponseEntity<Album> createAlbum(
            Principal principal,
            @RequestParam String name,
            @RequestParam(required = false) String description
    ) {
        try {
            Album album = albumService.createAlbum(principal.getName(), name, description);
            return ResponseEntity.ok(album);
        } catch (Exception e) {
            log.error("Error creating album", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{albumId}/photos/{photoId}")
    public ResponseEntity<Album> addPhotoToAlbum(
            @PathVariable String albumId,
            @PathVariable String photoId
    ) {
        try {
            Album album = albumService.addPhotoToAlbum(albumId, photoId);
            return ResponseEntity.ok(album);
        } catch (Exception e) {
            log.error("Error adding photo to album", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Album>> getUserAlbums(Principal principal) {
        try {
            List<Album> albums = albumService.getUserAlbums(principal.getName());
            return ResponseEntity.ok(albums);
        } catch (Exception e) {
            log.error("Error getting user albums", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{albumId}")
    public ResponseEntity<Album> getAlbum(@PathVariable String albumId) {
        try {
            Album album = albumService.getAlbumById(albumId);
            return ResponseEntity.ok(album);
        } catch (Exception e) {
            log.error("Error getting album", e);
            return ResponseEntity.notFound().build();
        }
    }
}