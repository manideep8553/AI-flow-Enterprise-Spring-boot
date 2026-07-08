package com.aiflow.enterprise.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.Tagging;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public String uploadFile(byte[] data, String fileName, String contentType, String prefix) {
        String key = prefix + "/" + UUID.randomUUID() + "_" + fileName;
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName).key(key)
                .contentType(contentType)
                .contentLength((long) data.length)
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(data));
        log.info("File uploaded to S3: bucket={} key={} size={}", bucketName, key, data.length);
        return key;
    }

    public String uploadFileWithEncryption(byte[] data, String fileName, String contentType,
                                            String prefix, String kmsKeyId) {
        String key = prefix + "/" + UUID.randomUUID() + "_" + fileName;
        PutObjectRequest.Builder builder = PutObjectRequest.builder()
                .bucket(bucketName).key(key)
                .contentType(contentType)
                .contentLength((long) data.length);

        if (kmsKeyId != null && !kmsKeyId.isBlank()) {
            builder.serverSideEncryption(ServerSideEncryption.AWS_KMS)
                    .ssekmsKeyId(kmsKeyId);
        } else {
            builder.serverSideEncryption(ServerSideEncryption.AES256);
        }

        s3Client.putObject(builder.build(), RequestBody.fromBytes(data));
        log.info("Encrypted file uploaded to S3: key={}", key);
        return key;
    }

    public String uploadFileWithTags(byte[] data, String fileName, String contentType,
                                      String prefix, Map<String, String> tags) {
        String key = prefix + "/" + UUID.randomUUID() + "_" + fileName;
        Tagging tagging = Tagging.builder()
                .tagSet(tags.entrySet().stream()
                        .map(e -> Tag.builder().key(e.getKey()).value(e.getValue()).build())
                        .collect(Collectors.toList()))
                .build();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName).key(key)
                .contentType(contentType)
                .contentLength((long) data.length)
                .tagging(tagging)
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(data));
        log.info("Tagged file uploaded to S3: key={} tags={}", key, tags);
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

    public String copyFile(String sourceKey, String destinationKey) {
        try {
            CopyObjectRequest request = CopyObjectRequest.builder()
                    .sourceBucket(bucketName).sourceKey(sourceKey)
                    .destinationBucket(bucketName).destinationKey(destinationKey)
                    .build();
            s3Client.copyObject(request);
            log.info("File copied from {} to {}", sourceKey, destinationKey);
            return destinationKey;
        } catch (S3Exception e) {
            log.error("Failed to copy file: key={} error={}", sourceKey, e.getMessage());
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

    public String getBucketName() {
        return bucketName;
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

    public String computeContentHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public String getStorageClass(String s3Key) {
        try {
            HeadObjectResponse response = s3Client.headObject(
                    HeadObjectRequest.builder().bucket(bucketName).key(s3Key).build());
            return response.storageClassAsString();
        } catch (S3Exception e) {
            log.warn("Failed to get storage class for key={}: {}", s3Key, e.getMessage());
            return null;
        }
    }
}
