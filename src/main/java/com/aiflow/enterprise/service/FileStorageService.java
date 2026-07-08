package com.aiflow.enterprise.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final S3Client s3Client;
    private final String bucketName;
    private final String cdnBaseUrl;

    public FileStorageService(S3Client s3Client,
                              @Value("${app.aws.s3.bucket-name:}") String bucketName,
                              @Value("${app.aws.s3.cdn-base-url:}") String cdnBaseUrl) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.cdnBaseUrl = cdnBaseUrl;
    }

    public String uploadFile(MultipartFile file, String folder) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        String key = folder + "/" + UUID.randomUUID() + extension;

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));
            log.info("File uploaded to S3: bucket={}, key={}", bucketName, key);

            if (!cdnBaseUrl.isBlank()) {
                return cdnBaseUrl + "/" + key;
            }
            return "https://" + bucketName + ".s3.amazonaws.com/" + key;

        } catch (IOException e) {
            log.error("Failed to upload file to S3: {}", e.getMessage());
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    public String uploadProfileImage(MultipartFile file, String userId) {
        validateImageFile(file);
        return uploadFile(file, "profiles/" + userId);
    }

    public void deleteFile(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);
            if (key != null) {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build());
                log.info("File deleted from S3: key={}", key);
            }
        } catch (Exception e) {
            log.error("Failed to delete file from S3: {}", e.getMessage());
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File size must not exceed 5MB");
        }
    }

    private String extractKeyFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return null;
        if (!cdnBaseUrl.isBlank() && fileUrl.startsWith(cdnBaseUrl)) {
            return fileUrl.substring(cdnBaseUrl.length() + 1);
        }
        String prefix = "https://" + bucketName + ".s3.amazonaws.com/";
        if (fileUrl.startsWith(prefix)) {
            return fileUrl.substring(prefix.length());
        }
        return null;
    }
}
