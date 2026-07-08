package com.aiflow.enterprise.repository;

import com.aiflow.enterprise.entity.Document;
import com.aiflow.enterprise.entity.embedded.LifecyclePolicy;
import com.aiflow.enterprise.enums.DocumentType;
import com.aiflow.enterprise.enums.ProcessingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends MongoRepository<Document, String> {

    Page<Document> findByUploadedBy(String uploadedBy, Pageable pageable);

    Page<Document> findByDocumentType(DocumentType documentType, Pageable pageable);

    Page<Document> findByProcessingStatus(ProcessingStatus status, Pageable pageable);

    Page<Document> findByDocumentTypeAndProcessingStatus(DocumentType type, ProcessingStatus status, Pageable pageable);

    Page<Document> findByOriginalNameContainingIgnoreCase(String search, Pageable pageable);

    Optional<Document> findByContentHash(String contentHash);

    List<Document> findByContentHashIn(List<String> contentHashes);

    List<Document> findByRequestId(String requestId);

    Page<Document> findByTagsContaining(String tag, Pageable pageable);

    Page<Document> findByCategory(String category, Pageable pageable);

    Page<Document> findByArchivedFalse(Pageable pageable);

    Page<Document> findByArchived(boolean archived, Pageable pageable);

    long countByDocumentType(DocumentType documentType);

    long countByProcessingStatus(ProcessingStatus status);

    long countByUploadedBy(String uploadedBy);

    long countByCategory(String category);

    boolean existsByContentHash(String contentHash);

    @Query("{'virusScanResult.status': ?0}")
    List<Document> findByVirusScanStatus(String status);

    @Query("{'lifecyclePolicy.expiresAt': {$lt: ?0}}")
    List<Document> findByLifecyclePolicyExpiresAtBefore(Instant now);

    @Query("{'storageInfo.storageClass': ?0}")
    List<Document> findByStorageClass(String storageClass);

    @Query("{'tags': {$in: ?0}}")
    Page<Document> findByTagsIn(List<String> tags, Pageable pageable);

    List<Document> findByS3Key(String s3Key);
}
