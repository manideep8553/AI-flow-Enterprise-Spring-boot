package com.aiflow.enterprise.service.impl;

import com.aiflow.enterprise.entity.Document;
import com.aiflow.enterprise.entity.embedded.DocumentVersion;
import com.aiflow.enterprise.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class DocumentVersionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentVersionService.class);

    private final DocumentRepository documentRepository;
    private final DocumentStorageService storageService;
    private final int maxVersions;

    public DocumentVersionService(DocumentRepository documentRepository,
                                   DocumentStorageService storageService,
                                   @Value("${app.document.version.max-versions:10}") int maxVersions) {
        this.documentRepository = documentRepository;
        this.storageService = storageService;
        this.maxVersions = maxVersions;
    }

    public Document createVersion(Document doc, byte[] fileData, String uploadedBy, String changeNotes) {
        DocumentVersion version = DocumentVersion.builder()
                .versionNumber(doc.getVersion())
                .s3Key(doc.getS3Key())
                .thumbnailS3Key(doc.getThumbnailS3Key())
                .contentHash(doc.getContentHash())
                .fileSize(doc.getFileSize())
                .mimeType(doc.getMimeType())
                .uploadedBy(uploadedBy)
                .uploadedAt(doc.getUpdatedAt() != null ? doc.getUpdatedAt() : doc.getCreatedAt())
                .changeNotes(changeNotes)
                .storageClass(doc.getStorageInfo() != null ? doc.getStorageInfo().getStorageClass() : null)
                .build();

        if (doc.getVersionHistory() == null) {
            doc.setVersionHistory(new ArrayList<>());
        }

        doc.getVersionHistory().add(version);
        doc.setVersion(doc.getVersion() + 1);

        doc.getVersionHistory().sort(Comparator.comparingInt(DocumentVersion::getVersionNumber));
        while (doc.getVersionHistory().size() > maxVersions) {
            DocumentVersion oldest = doc.getVersionHistory().remove(0);
            log.info("Removed old version {} from document {} to maintain max versions",
                    oldest.getVersionNumber(), doc.getId());
        }

        Document saved = documentRepository.save(doc);
        log.info("Version {} created for document {} by {}", version.getVersionNumber(), doc.getId(), uploadedBy);
        return saved;
    }

    public List<DocumentVersion> getVersionHistory(String documentId) {
        Document doc = documentRepository.findById(documentId).orElse(null);
        if (doc == null) return List.of();
        List<DocumentVersion> versions = new ArrayList<>(doc.getVersionHistory() != null
                ? doc.getVersionHistory() : List.of());
        versions.sort(Comparator.comparingInt(DocumentVersion::getVersionNumber));
        return versions;
    }

    public byte[] restoreVersion(String documentId, int versionNumber) {
        Document doc = documentRepository.findById(documentId).orElse(null);
        if (doc == null || doc.getVersionHistory() == null) return null;

        DocumentVersion targetVersion = doc.getVersionHistory().stream()
                .filter(v -> v.getVersionNumber() == versionNumber)
                .findFirst()
                .orElse(null);

        if (targetVersion == null || targetVersion.getS3Key() == null) return null;

        log.info("Restoring document {} to version {}", documentId, versionNumber);
        return storageService.downloadFile(targetVersion.getS3Key());
    }

    public DocumentVersion getLatestVersion(String documentId) {
        Document doc = documentRepository.findById(documentId).orElse(null);
        if (doc == null || doc.getVersionHistory() == null || doc.getVersionHistory().isEmpty()) {
            return null;
        }
        return doc.getVersionHistory().stream()
                .max(Comparator.comparingInt(DocumentVersion::getVersionNumber))
                .orElse(null);
    }

    public int getVersionCount(String documentId) {
        Document doc = documentRepository.findById(documentId).orElse(null);
        return doc != null && doc.getVersionHistory() != null ? doc.getVersionHistory().size() : 0;
    }
}
