package com.aiflow.enterprise.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class DocumentStorageService {

    private static final Logger log = LoggerFactory.getLogger(DocumentStorageService.class);

    private final S3Client s3Client;
    private final String bucketName;

    public DocumentStorageService(S3Client s3Client,
                                  @Value("${app.aws.s3.bucket-name:}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    public String uploadFile(MultipartFile file, String prefix) throws IOException {
        String key = prefix + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();
        s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        log.info("File uploaded to S3: bucket={} key={}", bucketName, key);
        return key;
    }

    public byte[] downloadFile(String s3Key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName).key(s3Key).build();
            ResponseBytes<GetObjectResponse> bytes = s3Client.getObjectAsBytes(request);
            return bytes.asByteArray();
        } catch (S3Exception e) {
            log.error("Failed to download file from S3: key={} error={}", s3Key, e.getMessage());
            return null;
        }
    }

    public void deleteFile(String s3Key) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName).key(s3Key).build();
            s3Client.deleteObject(request);
            log.info("File deleted from S3: key={}", s3Key);
        } catch (S3Exception e) {
            log.error("Failed to delete file from S3: key={} error={}", s3Key, e.getMessage());
        }
    }

    public String getPublicUrl(String s3Key) {
        return "https://" + bucketName + ".s3.amazonaws.com/" + s3Key;
    }

    public String computeContentHash(MultipartFile file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
