package com.example.photo_album.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Photo {
    private String id;
    private String fileName;
    private String contentType;
    private long size;
    private String url;
    private LocalDateTime uploadDate;
    private List<String> tags;
    private Map<String, Float> labels;  // Rekognition'dan gelen etiketler
}