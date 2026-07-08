package com.aiflow.enterprise.service.impl;

import com.aiflow.enterprise.entity.Document;
import com.aiflow.enterprise.entity.embedded.LifecyclePolicy;
import com.aiflow.enterprise.entity.embedded.StorageInfo;
import com.aiflow.enterprise.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.StorageClass;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class DocumentLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(DocumentLifecycleService.class);

    private final DocumentRepository documentRepository;
    private final S3Client s3Client;
    private final String bucketName;
    private final int defaultRetentionDays;

    public DocumentLifecycleService(DocumentRepository documentRepository,
                                     S3Client s3Client,
                                     @Value("${app.aws.s3.bucket-name:}") String bucketName,
                                     @Value("${app.document.lifecycle.default-retention-days:365}") int defaultRetentionDays) {
        this.documentRepository = documentRepository;
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.defaultRetentionDays = defaultRetentionDays;
    }

    public LifecyclePolicy createPolicy(int retentionDays, LifecyclePolicy.LifecycleAction action) {
        return LifecyclePolicy.builder()
                .retentionDays(retentionDays > 0 ? retentionDays : defaultRetentionDays)
                .expiresAt(Instant.now().plus(
                        retentionDays > 0 ? retentionDays : defaultRetentionDays, ChronoUnit.DAYS))
                .action(action != null ? action : LifecyclePolicy.LifecycleAction.DELETE)
                .notificationSent(false)
                .build();
    }

    public LifecyclePolicy createDefaultPolicy() {
        return createPolicy(defaultRetentionDays, LifecyclePolicy.LifecycleAction.ARCHIVE_TO_GLACIER);
    }

    public int enforceLifecyclePolicies() {
        Instant now = Instant.now();
        int processed = 0;

        List<Document> expiredDocs = documentRepository.findByLifecyclePolicyExpiresAtBefore(now);
        for (Document doc : expiredDocs) {
            try {
                LifecyclePolicy policy = doc.getLifecyclePolicy();
                if (policy == null) continue;

                switch (policy.getAction()) {
                    case DELETE -> deleteExpiredDocument(doc);
                    case ARCHIVE_TO_GLACIER -> moveToGlacier(doc);
                    case ARCHIVE_TO_DEEP_ARCHIVE -> moveToDeepArchive(doc);
                    case MOVE_TO_STANDARD_IA -> moveToStandardIa(doc);
                    case KEEP -> refreshExpiry(doc);
                }
                processed++;
            } catch (Exception e) {
                log.warn("Failed to enforce lifecycle for document {}: {}", doc.getId(), e.getMessage());
            }
        }

        log.info("Lifecycle enforcement complete: {} documents processed", processed);
        return processed;
    }

    private void deleteExpiredDocument(Document doc) {
        if (doc.getS3Key() != null) {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName).key(doc.getS3Key()).build());
        }
        if (doc.getThumbnailS3Key() != null) {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName).key(doc.getThumbnailS3Key()).build());
        }
        documentRepository.delete(doc);
        log.info("Deleted expired document: id={} name={}", doc.getId(), doc.getOriginalName());
    }

    private void transitionStorageClass(Document doc, StorageClass targetClass) {
        if (doc.getS3Key() == null) return;

        CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                .sourceBucket(bucketName).sourceKey(doc.getS3Key())
                .destinationBucket(bucketName).destinationKey(doc.getS3Key())
                .storageClass(targetClass)
                .build();
        s3Client.copyObject(copyRequest);

        String storageClassStr = targetClass.toString();
        if (doc.getStorageInfo() == null) {
            doc.setStorageInfo(StorageInfo.builder().storageClass(storageClassStr).build());
        } else {
            doc.getStorageInfo().setStorageClass(storageClassStr);
            doc.getStorageInfo().setOptimizedAt(Instant.now());
        }
        doc.setLifecyclePolicy(null);
        documentRepository.save(doc);
        log.info("Transitioned document {} to storage class: {}", doc.getId(), storageClassStr);
    }

    private void moveToGlacier(Document doc) {
        transitionStorageClass(doc, StorageClass.GLACIER);
    }

    private void moveToDeepArchive(Document doc) {
        transitionStorageClass(doc, StorageClass.DEEP_ARCHIVE);
    }

    private void moveToStandardIa(Document doc) {
        transitionStorageClass(doc, StorageClass.STANDARD_IA);
    }

    public boolean shouldTransitionToIa(Document doc) {
        if (doc.getStorageInfo() != null && doc.getStorageInfo().getStorageClass() != null) {
            return false;
        }
        Instant accessed = doc.getUpdatedAt() != null ? doc.getUpdatedAt() : doc.getCreatedAt();
        return accessed != null && ChronoUnit.DAYS.between(accessed, Instant.now()) > 90;
    }

    private void refreshExpiry(Document doc) {
        if (doc.getLifecyclePolicy() != null) {
            doc.getLifecyclePolicy().setExpiresAt(
                    Instant.now().plus(doc.getLifecyclePolicy().getRetentionDays(), ChronoUnit.DAYS));
            doc.getLifecyclePolicy().setNotificationSent(false);
            documentRepository.save(doc);
            log.debug("Refreshed lifecycle expiry for document: {}", doc.getId());
        }
    }
}
