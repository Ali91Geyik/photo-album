// src/main/java/com/example/photo_album/service/AlbumService.java
package com.example.photo_album.service;

import com.example.photo_album.model.Album;
import com.example.photo_album.model.Photo;
import com.example.photo_album.model.User;
import com.example.photo_album.repository.AlbumRepository;
import com.example.photo_album.repository.PhotoRepository;
import com.example.photo_album.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Slf4j
public class AlbumService {
    private final AlbumRepository albumRepository;
    private final UserRepository userRepository;
    private final PhotoRepository photoRepository;

    public AlbumService(
            AlbumRepository albumRepository,
            UserRepository userRepository,
            PhotoRepository photoRepository
    ) {
        this.albumRepository = albumRepository;
        this.userRepository = userRepository;
        this.photoRepository = photoRepository;
    }

    @Transactional
    public Album createAlbum(String userId, String name, String description) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        Album album = Album.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .description(description)
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();

        return albumRepository.save(album);
    }

    @Transactional
    public Album addPhotoToAlbum(String albumId, String photoId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new NoSuchElementException("Album not found"));

        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new NoSuchElementException("Photo not found"));

        // Ensure photo belongs to same user as album
        if (!album.getUser().getId().equals(photo.getUser().getId())) {
            throw new IllegalArgumentException("Photo does not belong to album owner");
        }

        album.getPhotos().add(photo);
        return albumRepository.save(album);
    }

    @Transactional(readOnly = true)
    public List<Album> getUserAlbums(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        
        return albumRepository.findByUser(user);
    }

    @Transactional(readOnly = true)
    public Album getAlbumById(String albumId) {
        return albumRepository.findById(albumId)
                .orElseThrow(() -> new NoSuchElementException("Album not found"));
    }
}