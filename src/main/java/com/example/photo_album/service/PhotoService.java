package com.example.photo_album.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.example.photo_album.model.Photo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class PhotoService {
    private final AmazonS3 amazonS3;
    private final String bucketName;

    public PhotoService(AmazonS3 amazonS3, @Value("${aws.s3.bucket}") String bucketName) {
        this.amazonS3 = amazonS3;
        this.bucketName = bucketName;
    }

    public Photo uploadPhoto(MultipartFile file) throws IOException {
        String fileName = generateUniqueFileName(file.getOriginalFilename());

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        amazonS3.putObject(bucketName, fileName, file.getInputStream(), metadata);

        return Photo.builder()
                .id(UUID.randomUUID().toString())
                .fileName(fileName)
                .contentType(file.getContentType())
                .size(file.getSize())
                .url(generateUrl(fileName))
                .uploadDate(LocalDateTime.now())
                .build();
    }

    private String generateUniqueFileName(String originalFileName) {
        return UUID.randomUUID().toString() + "_" + originalFileName;
    }

    private String generateUrl(String fileName) {
        return amazonS3.getUrl(bucketName, fileName).toString();
    }
}