// src/main/java/com/example/photo_album/repository/AlbumRepository.java
package com.example.photo_album.repository;

import com.example.photo_album.model.Album;
import com.example.photo_album.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlbumRepository extends JpaRepository<Album, String> {
    List<Album> findByUser(User user);
    List<Album> findByUserAndNameContainingIgnoreCase(User user, String name);
}