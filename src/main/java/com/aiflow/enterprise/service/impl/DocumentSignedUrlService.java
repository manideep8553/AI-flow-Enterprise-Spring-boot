package com.aiflow.enterprise.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class DocumentSignedUrlService {

    private static final Logger log = LoggerFactory.getLogger(DocumentSignedUrlService.class);

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final String bucketName;
    private final long defaultExpirySeconds;

    public DocumentSignedUrlService(S3Presigner s3Presigner,
                                     S3Client s3Client,
                                     @Value("${app.aws.s3.bucket-name:}") String bucketName,
                                     @Value("${app.document.signed-url.expiry-seconds:900}") long defaultExpirySeconds) {
        this.s3Presigner = s3Presigner;
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.defaultExpirySeconds = defaultExpirySeconds;
    }

    public URL generateDownloadUrl(String s3Key) {
        return generateDownloadUrl(s3Key, defaultExpirySeconds);
    }

    public URL generateDownloadUrl(String s3Key, long expirySeconds) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .responseContentDisposition("inline")
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(expirySeconds))
                    .getObjectRequest(getRequest)
                    .build();

            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
            URL url = presigned.url();

            log.debug("Generated signed download URL for key={} expires={}s", s3Key, expirySeconds);
            return url;
        } catch (Exception e) {
            log.error("Failed to generate signed download URL for key={}: {}", s3Key, e.getMessage());
            return null;
        }
    }

    public URL generateDownloadUrl(String s3Key, long expirySeconds, String contentDisposition) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .responseContentDisposition(contentDisposition)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(expirySeconds))
                    .getObjectRequest(getRequest)
                    .build();

            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
            return presigned.url();
        } catch (Exception e) {
            log.error("Failed to generate signed download URL: {}", e.getMessage());
            return null;
        }
    }

    public Map<String, Object> generateUploadUrl(String fileName, String contentType) {
        return generateUploadUrl(fileName, contentType, defaultExpirySeconds);
    }

    public Map<String, Object> generateUploadUrl(String fileName, String contentType, long expirySeconds) {
        try {
            String s3Key = "uploads/" + java.util.UUID.randomUUID() + "_" + fileName;

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(contentType)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(expirySeconds))
                    .putObjectRequest(putRequest)
                    .build();

            PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);

            Map<String, Object> result = new HashMap<>();
            result.put("url", presigned.url().toString());
            result.put("s3Key", s3Key);
            result.put("bucket", bucketName);
            result.put("expiresIn", expirySeconds);
            result.put("headers", presigned.signedHeaders());

            log.info("Generated signed upload URL for file={} key={}", fileName, s3Key);
            return result;
        } catch (Exception e) {
            log.error("Failed to generate signed upload URL: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    public URL generatePreviewUrl(String s3Key) {
        return generateDownloadUrl(s3Key, 3600, "inline");
    }
}
