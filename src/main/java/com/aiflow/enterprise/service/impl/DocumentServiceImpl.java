package com.aiflow.enterprise.service.impl;

import com.aiflow.enterprise.dto.request.DocumentUploadRequest;
import com.aiflow.enterprise.dto.response.DocumentResponse;
import com.aiflow.enterprise.entity.Document;
import com.aiflow.enterprise.entity.Request;
import com.aiflow.enterprise.entity.embedded.*;
import com.aiflow.enterprise.enums.DocumentType;
import com.aiflow.enterprise.enums.ProcessingStatus;
import com.aiflow.enterprise.exception.ResourceNotFoundException;
import com.aiflow.enterprise.mapper.DocumentMapper;
import com.aiflow.enterprise.repository.DocumentRepository;
import com.aiflow.enterprise.repository.RequestRepository;
import com.aiflow.enterprise.service.DocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class DocumentServiceImpl implements DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentServiceImpl.class);

    private final DocumentRepository documentRepository;
    private final DocumentMapper documentMapper;
    private final DocumentStorageService storageService;
    private final DocumentAIProcessor aiProcessor;
    private final DocumentValidationService validationService;
    private final DocumentVirusScanService virusScanService;
    private final DocumentThumbnailService thumbnailService;
    private final DocumentPreviewService previewService;
    private final DocumentSignedUrlService signedUrlService;
    private final DocumentLifecycleService lifecycleService;
    private final DocumentStorageOptimizer storageOptimizer;
    private final DocumentCategorizationService categorizationService;
    private final DocumentVersionService versionService;
    private final RequestRepository requestRepository;
    private final ObjectMapper objectMapper;

    public DocumentServiceImpl(DocumentRepository documentRepository,
                                DocumentMapper documentMapper,
                                DocumentStorageService storageService,
                                DocumentAIProcessor aiProcessor,
                                DocumentValidationService validationService,
                                DocumentVirusScanService virusScanService,
                                DocumentThumbnailService thumbnailService,
                                DocumentPreviewService previewService,
                                DocumentSignedUrlService signedUrlService,
                                DocumentLifecycleService lifecycleService,
                                DocumentStorageOptimizer storageOptimizer,
                                DocumentCategorizationService categorizationService,
                                DocumentVersionService versionService,
                                RequestRepository requestRepository,
                                ObjectMapper objectMapper) {
        this.documentRepository = documentRepository;
        this.documentMapper = documentMapper;
        this.storageService = storageService;
        this.aiProcessor = aiProcessor;
        this.validationService = validationService;
        this.virusScanService = virusScanService;
        this.thumbnailService = thumbnailService;
        this.previewService = previewService;
        this.signedUrlService = signedUrlService;
        this.lifecycleService = lifecycleService;
        this.storageOptimizer = storageOptimizer;
        this.categorizationService = categorizationService;
        this.versionService = versionService;
        this.requestRepository = requestRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public DocumentResponse uploadDocument(MultipartFile file, DocumentUploadRequest uploadRequest, String userId) {
        try {
            String contentHash = storageService.computeContentHash(file);
            Optional<Document> existing = documentRepository.findByContentHash(contentHash);
            if (existing.isPresent()) {
                log.info("Duplicate document detected: {} matches {}", file.getOriginalFilename(),
                        existing.get().getOriginalName());
                return documentMapper.toResponse(existing.get());
            }

            byte[] fileData = file.getBytes();
            boolean useCompression = uploadRequest != null && uploadRequest.isEnableCompression();
            byte[] storageData = useCompression ? storageOptimizer.maybeCompress(fileData, file.getContentType(),
                    file.getOriginalFilename()) : fileData;

            String s3Key;
            if (uploadRequest != null && uploadRequest.isEnableEncryption()) {
                s3Key = storageService.uploadFileWithEncryption(storageData, file.getOriginalFilename(),
                        file.getContentType(), "documents", uploadRequest.getKmsKeyId());
            } else {
                s3Key = storageService.uploadFile(storageData, file.getOriginalFilename(),
                        file.getContentType(), "documents");
            }

            StorageInfo storageInfo = storageOptimizer.analyzeStorage(
                    Document.builder().fileSize(file.getSize()).build(), fileData, storageData);

            Document doc = Document.builder()
                    .fileName(s3Key.substring(s3Key.lastIndexOf('/') + 1))
                    .originalName(file.getOriginalFilename())
                    .mimeType(file.getContentType())
                    .fileSize(file.getSize())
                    .processingStatus(ProcessingStatus.UPLOADED)
                    .uploadedBy(userId)
                    .uploadedAt(Instant.now())
                    .s3Bucket(storageService.getBucketName())
                    .s3Key(s3Key)
                    .s3Url(storageService.getPublicUrl(s3Key))
                    .contentHash(contentHash)
                    .storageInfo(storageInfo)
                    .category(uploadRequest != null ? uploadRequest.getCategory() : null)
                    .tags(uploadRequest != null ? uploadRequest.getTags() : null)
                    .notes(uploadRequest != null ? uploadRequest.getNotes() : null)
                    .requestId(uploadRequest != null ? uploadRequest.getRequestId() : null)
                    .requestTypeId(uploadRequest != null ? uploadRequest.getRequestTypeId() : null)
                    .pageCount(1)
                    .version(1)
                    .archived(false)
                    .build();

            if (uploadRequest != null && uploadRequest.getRetentionDays() > 0) {
                doc.setLifecyclePolicy(lifecycleService.createPolicy(
                        uploadRequest.getRetentionDays(), uploadRequest.getLifecycleAction()));
            }

            if (uploadRequest != null && uploadRequest.isEnableEncryption()) {
                doc.setEncryptionInfo(EncryptionInfo.builder()
                        .enabled(true)
                        .algorithm(uploadRequest.getKmsKeyId() != null ? "SSE-KMS" : "SSE-S3")
                        .kmsKeyId(uploadRequest.getKmsKeyId())
                        .encryptedAt(Instant.now())
                        .status("ENCRYPTED")
                        .build());
            }

            Document saved = documentRepository.save(doc);
            log.info("Document uploaded: id={} name={} size={}", saved.getId(), saved.getOriginalName(), saved.getFileSize());

            processDocumentAsync(saved.getId(), fileData);

            return documentMapper.toResponse(saved);
        } catch (Exception e) {
            log.error("Document upload failed: {}", e.getMessage(), e);
            Document failed = Document.builder()
                    .originalName(file.getOriginalFilename())
                    .mimeType(file.getContentType())
                    .fileSize(file.getSize())
                    .processingStatus(ProcessingStatus.FAILED)
                    .uploadedBy(userId)
                    .uploadedAt(Instant.now())
                    .notes("Upload failed: " + e.getMessage())
                    .build();
            Document saved = documentRepository.save(failed);
            return documentMapper.toResponse(saved);
        }
    }

    @Override
    public DocumentResponse updateDocument(String id, MultipartFile file, DocumentUploadRequest updateRequest,
                                            String userId) {
        Document doc = findOrThrow(id);

        if (file != null && !file.isEmpty()) {
            try {
                byte[] fileData = file.getBytes();
                String contentHash = storageService.computeContentHash(file);

                doc = versionService.createVersion(doc, fileData, userId,
                        updateRequest != null ? updateRequest.getChangeNotes() : "Updated file");

                String s3Key = storageService.uploadFile(fileData, file.getOriginalFilename(),
                        file.getContentType(), "documents");
                doc.setS3Key(s3Key);
                doc.setS3Url(storageService.getPublicUrl(s3Key));
                doc.setContentHash(contentHash);
                doc.setFileSize(file.getSize());
                doc.setMimeType(file.getContentType());
                doc.setOriginalName(file.getOriginalFilename());
                doc.setFileName(s3Key.substring(s3Key.lastIndexOf('/') + 1));
                doc.setProcessingStatus(ProcessingStatus.UPLOADED);
                doc.setUploadedAt(Instant.now());

                Document saved = documentRepository.save(doc);
                processDocumentAsync(saved.getId(), fileData);
                return documentMapper.toResponse(saved);

            } catch (Exception e) {
                log.error("Document update failed: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to update document", e);
            }
        }

        if (updateRequest != null) {
            if (updateRequest.getCategory() != null) doc.setCategory(updateRequest.getCategory());
            if (updateRequest.getTags() != null) doc.setTags(updateRequest.getTags());
            if (updateRequest.getNotes() != null) doc.setNotes(updateRequest.getNotes());
            if (updateRequest.getRetentionDays() > 0) {
                doc.setLifecyclePolicy(lifecycleService.createPolicy(
                        updateRequest.getRetentionDays(), updateRequest.getLifecycleAction()));
            }
        }

        Document saved = documentRepository.save(doc);
        return documentMapper.toResponse(saved);
    }

    @Async("workflowExecutor")
    public void processDocumentAsync(String documentId, byte... fileData) {
        log.info("Processing document: {}", documentId);
        Document doc = documentRepository.findById(documentId).orElse(null);
        if (doc == null) return;

        try {
            doc.setProcessingStatus(ProcessingStatus.PROCESSING);
            documentRepository.save(doc);

            byte[] data = fileData != null && fileData.length > 0
                    ? fileData : storageService.downloadFile(doc.getS3Key());
            if (data == null || data.length == 0) {
                doc.setProcessingStatus(ProcessingStatus.FAILED);
                documentRepository.save(doc);
                return;
            }

            VirusScanResult virusResult = virusScanService.scan(data, doc.getOriginalName(), doc.getMimeType());
            doc.setVirusScanResult(virusResult);
            if (virusResult.getStatus() == VirusScanResult.VirusScanStatus.INFECTED
                    || virusResult.getStatus() == VirusScanResult.VirusScanStatus.QUARANTINED) {
                doc.setProcessingStatus(ProcessingStatus.FAILED);
                doc.setNotes("File rejected: " + virusResult.getThreatName());
                documentRepository.save(doc);
                log.warn("Infected file rejected: id={} threat={}", doc.getId(), virusResult.getThreatName());
                return;
            }

            DocumentAIProcessor.DocumentTypeResult typeResult = aiProcessor.classifyDocument(data, doc);
            doc.setDocumentType(typeResult.documentType());
            doc.setDocumentTypeConfidence(typeResult.confidence());
            doc.setProcessingStatus(ProcessingStatus.OCR_COMPLETED);
            documentRepository.save(doc);

            String ocrText = aiProcessor.performOCR(data);
            doc.setOcrText(ocrText);
            doc.setOcrMethod("AWS_Textract");
            documentRepository.save(doc);

            List<ExtractedField> fields = aiProcessor.extractFields(typeResult.documentType(), ocrText, doc);
            doc.setExtractedFields(fields);

            Map<String, Object> extractedData = new HashMap<>();
            for (ExtractedField f : fields) {
                extractedData.put(f.getName(), f.getValue());
            }
            doc.setExtractedData(extractedData);
            doc.setProcessingStatus(ProcessingStatus.AI_EXTRACTED);
            documentRepository.save(doc);

            DocumentCategorizationService.CategorizationResult catResult =
                    categorizationService.categorize(doc, fields, ocrText);
            doc.setCategory(catResult.category());
            doc.setCategoryConfidence(catResult.confidence());
            if (doc.getTags() == null) doc.setTags(new ArrayList<>());
            for (String tag : catResult.suggestedTags()) {
                if (!doc.getTags().contains(tag)) doc.getTags().add(tag);
            }

            String analysis = aiProcessor.analyzeDocument(ocrText, typeResult.documentType());
            doc.setAiAnalysis(analysis);

            String summary = aiProcessor.generateSummary(typeResult.documentType(), fields, ocrText);
            doc.setSummary(summary);

            List<DocumentValidationResult> validations = validationService.validate(typeResult.documentType(), fields);
            doc.setValidationResults(validations);

            List<AnomalyResult> anomalies = aiProcessor.detectAnomalies(doc, fields);
            doc.setAnomalies(anomalies);

            DuplicateInfo duplicateInfo = aiProcessor.checkDuplicate(doc, fields);
            doc.setDuplicateInfo(duplicateInfo);

            byte[] thumbnailData = thumbnailService.generateThumbnail(data, doc.getMimeType(), doc.getOriginalName());
            if (thumbnailData != null) {
                String thumbKey = storageService.uploadFile(thumbnailData,
                        "thumb_" + doc.getFileName(), "image/png", "thumbnails");
                doc.setThumbnailS3Key(thumbKey);
            }

            DocumentPreviewService.PreviewResult preview = previewService.generatePreview(
                    data, doc.getMimeType(), doc.getOriginalName());
            if (preview.pages() != null && !preview.pages().isEmpty()) {
                String previewKey = storageService.uploadFile(objectMapper.writeValueAsBytes(preview.pages()),
                        "preview_" + doc.getId() + ".json", "application/json", "previews");
                doc.setPreviewS3Key(previewKey);
                doc.setPageCount(preview.totalPages());
            }

            if (doc.getLifecyclePolicy() == null) {
                doc.setLifecyclePolicy(lifecycleService.createDefaultPolicy());
            }

            if (duplicateInfo != null && duplicateInfo.isDuplicate()) {
                doc.setProcessingStatus(ProcessingStatus.DUPLICATE_DETECTED);
            } else if (!anomalies.isEmpty()) {
                doc.setProcessingStatus(ProcessingStatus.ANOMALY_DETECTED);
            } else {
                doc.setProcessingStatus(ProcessingStatus.VALIDATED);
            }

            doc.setProcessedAt(Instant.now());
            documentRepository.save(doc);

            if (doc.getRequestId() != null && fields != null && !fields.isEmpty()) {
                autoFillRequest(doc, fields);
            }

            log.info("Document processed: id={} type={} status={} fields={} category={}",
                    doc.getId(), doc.getDocumentType(), doc.getProcessingStatus(),
                    fields != null ? fields.size() : 0, doc.getCategory());

        } catch (Exception e) {
            log.error("Document processing failed for {}: {}", documentId, e.getMessage(), e);
            doc.setProcessingStatus(ProcessingStatus.FAILED);
            documentRepository.save(doc);
        }
    }

    private void autoFillRequest(Document doc, List<ExtractedField> fields) {
        try {
            Request request = requestRepository.findById(doc.getRequestId()).orElse(null);
            if (request == null) return;

            Map<String, Object> formData = aiProcessor.autoFillRequestForm(doc, fields, doc.getRequestTypeId());
            if (formData != null && !formData.isEmpty()) {
                if (request.getFields() == null) request.setFields(new HashMap<>());
                request.getFields().putAll(formData);
                requestRepository.save(request);
                log.info("Request auto-filled: requestId={} fields={}", doc.getRequestId(), formData.size());
            }
        } catch (Exception e) {
            log.warn("Auto-fill failed for request {}: {}", doc.getRequestId(), e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(String id) {
        return documentMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponse> getAllDocuments(int page, int size, String documentType,
                                                   String processingStatus, String uploadedBy,
                                                   String search, String tag, String category, Boolean archived) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Document> docPage;

        if (documentType != null && processingStatus != null) {
            docPage = documentRepository.findByDocumentTypeAndProcessingStatus(
                    DocumentType.valueOf(documentType.toUpperCase()),
                    ProcessingStatus.valueOf(processingStatus.toUpperCase()), pageable);
        } else if (documentType != null) {
            docPage = documentRepository.findByDocumentType(
                    DocumentType.valueOf(documentType.toUpperCase()), pageable);
        } else if (processingStatus != null) {
            docPage = documentRepository.findByProcessingStatus(
                    ProcessingStatus.valueOf(processingStatus.toUpperCase()), pageable);
        } else if (uploadedBy != null) {
            docPage = documentRepository.findByUploadedBy(uploadedBy, pageable);
        } else if (search != null) {
            docPage = documentRepository.findByOriginalNameContainingIgnoreCase(search, pageable);
        } else if (tag != null) {
            docPage = documentRepository.findByTagsContaining(tag, pageable);
        } else if (category != null) {
            docPage = documentRepository.findByCategory(category, pageable);
        } else if (Boolean.FALSE.equals(archived)) {
            docPage = documentRepository.findByArchivedFalse(pageable);
        } else if (Boolean.TRUE.equals(archived)) {
            docPage = documentRepository.findByArchived(true, pageable);
        } else {
            docPage = documentRepository.findAll(pageable);
        }

        return docPage.map(doc -> {
            DocumentResponse resp = documentMapper.toResponse(doc);
            if (doc.getS3Key() != null) {
                resp.setDownloadUrl(signedUrlService.generateDownloadUrl(doc.getS3Key()).toString());
            }
            return resp;
        });
    }

    @Override
    public DocumentResponse reprocessDocument(String id) {
        Document doc = findOrThrow(id);
        doc.setProcessingStatus(ProcessingStatus.QUEUED);
        documentRepository.save(doc);
        processDocumentAsync(id);
        return documentMapper.toResponse(doc);
    }

    @Override
    public DocumentResponse archiveDocument(String id) {
        Document doc = findOrThrow(id);
        doc.setArchived(true);
        Document saved = documentRepository.save(doc);
        log.info("Document archived: id={}", id);
        return documentMapper.toResponse(saved);
    }

    @Override
    public DocumentResponse restoreDocument(String id) {
        Document doc = findOrThrow(id);
        doc.setArchived(false);
        Document saved = documentRepository.save(doc);
        log.info("Document restored: id={}", id);
        return documentMapper.toResponse(saved);
    }

    @Override
    public void deleteDocument(String id) {
        Document doc = findOrThrow(id);
        if (doc.getS3Key() != null) storageService.deleteFile(doc.getS3Key());
        if (doc.getThumbnailS3Key() != null) storageService.deleteFile(doc.getThumbnailS3Key());
        if (doc.getPreviewS3Key() != null) storageService.deleteFile(doc.getPreviewS3Key());
        if (doc.getVersionHistory() != null) {
            for (DocumentVersion v : doc.getVersionHistory()) {
                if (v.getS3Key() != null) storageService.deleteFile(v.getS3Key());
                if (v.getThumbnailS3Key() != null) storageService.deleteFile(v.getThumbnailS3Key());
            }
        }
        documentRepository.delete(doc);
        log.info("Document deleted: {} including {} versions", id,
                doc.getVersionHistory() != null ? doc.getVersionHistory().size() : 0);
    }

    @Override
    public void bulkDelete(List<String> ids) {
        for (String id : ids) {
            try {
                deleteDocument(id);
            } catch (Exception e) {
                log.warn("Failed to delete document {} in bulk operation: {}", id, e.getMessage());
            }
        }
        log.info("Bulk delete complete: {} documents", ids.size());
    }

    @Override
    public byte[] getDocumentFile(String id) {
        Document doc = findOrThrow(id);
        if (doc.getS3Key() == null) return null;
        byte[] data = storageService.downloadFile(doc.getS3Key());
        if (doc.getStorageInfo() != null && "gzip".equals(doc.getStorageInfo().getCompressionAlgorithm())) {
            data = storageOptimizer.maybeDecompress(data, doc.getStorageInfo().getStorageClass(),
                    doc.getStorageInfo().getCompressionAlgorithm());
        }
        return data;
    }

    @Override
    public String getDocumentDownloadUrl(String id) {
        Document doc = findOrThrow(id);
        if (doc.getS3Key() == null) return null;
        java.net.URL url = signedUrlService.generateDownloadUrl(doc.getS3Key());
        return url != null ? url.toString() : null;
    }

    @Override
    public String getDocumentDownloadUrl(String id, long expirySeconds) {
        Document doc = findOrThrow(id);
        if (doc.getS3Key() == null) return null;
        java.net.URL url = signedUrlService.generateDownloadUrl(doc.getS3Key(), expirySeconds);
        return url != null ? url.toString() : null;
    }

    @Override
    public String getDocumentPreviewUrl(String id) {
        Document doc = findOrThrow(id);
        if (doc.getPreviewS3Key() != null) {
            java.net.URL url = signedUrlService.generatePreviewUrl(doc.getPreviewS3Key());
            return url != null ? url.toString() : null;
        }
        if (doc.getS3Key() != null) {
            java.net.URL url = signedUrlService.generatePreviewUrl(doc.getS3Key());
            return url != null ? url.toString() : null;
        }
        return null;
    }

    @Override
    public Map<String, Object> getDocumentUploadUrl(String fileName, String contentType) {
        return signedUrlService.generateUploadUrl(fileName, contentType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentVersion> getDocumentVersions(String id) {
        return versionService.getVersionHistory(id);
    }

    @Override
    public DocumentResponse restoreDocumentVersion(String id, int versionNumber) {
        Document doc = findOrThrow(id);
        byte[] versionData = versionService.restoreVersion(id, versionNumber);
        if (versionData == null) {
            throw new ResourceNotFoundException("DocumentVersion", "versionNumber",
                    String.valueOf(versionNumber));
        }

        DocumentVersion version = doc.getVersionHistory().stream()
                .filter(v -> v.getVersionNumber() == versionNumber)
                .findFirst().orElse(null);

        doc = versionService.createVersion(doc, versionData, "system",
                "Restored from version " + versionNumber);

        String s3Key = storageService.uploadFile(versionData, doc.getOriginalName(),
                doc.getMimeType(), "documents");
        doc.setS3Key(s3Key);
        doc.setS3Url(storageService.getPublicUrl(s3Key));
        if (version != null) {
            doc.setContentHash(version.getContentHash());
            doc.setFileSize(version.getFileSize());
        }
        doc.setProcessingStatus(ProcessingStatus.UPLOADED);

        Document saved = documentRepository.save(doc);
        log.info("Document restored to version {}: id={}", versionNumber, id);
        return documentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public long getDocumentCountByType(String documentType) {
        return documentRepository.countByDocumentType(DocumentType.valueOf(documentType.toUpperCase()));
    }

    @Override
    @Transactional(readOnly = true)
    public long getDocumentCountByStatus(String processingStatus) {
        return documentRepository.countByProcessingStatus(ProcessingStatus.valueOf(processingStatus.toUpperCase()));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getDocumentStatistics() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total", documentRepository.count());
        for (DocumentType type : DocumentType.values()) {
            long count = documentRepository.countByDocumentType(type);
            if (count > 0) stats.put("type_" + type.name().toLowerCase(), count);
        }
        for (ProcessingStatus status : ProcessingStatus.values()) {
            long count = documentRepository.countByProcessingStatus(status);
            if (count > 0) stats.put("status_" + status.name().toLowerCase(), count);
        }
        return stats;
    }

    private Document findOrThrow(String id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));
    }
}
