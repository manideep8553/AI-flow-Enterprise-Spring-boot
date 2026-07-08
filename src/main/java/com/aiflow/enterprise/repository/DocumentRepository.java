package com.aiflow.enterprise.repository;

import com.aiflow.enterprise.entity.Document;
import com.aiflow.enterprise.enums.DocumentType;
import com.aiflow.enterprise.enums.ProcessingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

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

    Page<Document> findByArchivedFalse(Pageable pageable);

    long countByDocumentType(DocumentType documentType);

    long countByProcessingStatus(ProcessingStatus status);

    long countByUploadedBy(String uploadedBy);

    boolean existsByContentHash(String contentHash);
}
