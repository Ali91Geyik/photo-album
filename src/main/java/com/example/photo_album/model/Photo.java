// src/main/java/com/example/photo_album/model/Photo.java
package com.example.photo_album.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "photos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Photo {
    @Id
    private String id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long size;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private LocalDateTime uploadDate;

    @ElementCollection
    @CollectionTable(name = "photo_tags", joinColumns = @JoinColumn(name = "photo_id"))
    @Column(name = "tag")
    private List<String> tags;

    @ElementCollection
    @CollectionTable(name = "photo_labels", joinColumns = @JoinColumn(name = "photo_id"))
    @MapKeyColumn(name = "label_name")
    @Column(name = "confidence")
    private Map<String, Float> labels;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToMany(mappedBy = "photos")
    private List<Album> albums = new ArrayList<>();
}