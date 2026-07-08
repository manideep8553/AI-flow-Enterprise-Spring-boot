package com.aiflow.enterprise.service.impl;

import com.aiflow.enterprise.dto.request.DocumentUploadRequest;
import com.aiflow.enterprise.dto.response.DocumentResponse;
import com.aiflow.enterprise.entity.Document;
import com.aiflow.enterprise.entity.Request;
import com.aiflow.enterprise.entity.embedded.AnomalyResult;
import com.aiflow.enterprise.entity.embedded.DocumentValidationResult;
import com.aiflow.enterprise.entity.embedded.DuplicateInfo;
import com.aiflow.enterprise.entity.embedded.ExtractedField;
import com.aiflow.enterprise.enums.DocumentType;
import com.aiflow.enterprise.enums.ProcessingStatus;
import com.aiflow.enterprise.exception.ResourceNotFoundException;
import com.aiflow.enterprise.mapper.DocumentMapper;
import com.aiflow.enterprise.repository.DocumentRepository;
import com.aiflow.enterprise.repository.RequestRepository;
import com.aiflow.enterprise.service.DocumentService;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class DocumentServiceImpl implements DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentServiceImpl.class);

    private final DocumentRepository documentRepository;
    private final DocumentMapper documentMapper;
    private final DocumentStorageService storageService;
    private final DocumentAIProcessor aiProcessor;
    private final DocumentValidationService validationService;
    private final RequestRepository requestRepository;
    private final ObjectMapper objectMapper;

    public DocumentServiceImpl(DocumentRepository documentRepository,
                               DocumentMapper documentMapper,
                               DocumentStorageService storageService,
                               DocumentAIProcessor aiProcessor,
                               DocumentValidationService validationService,
                               RequestRepository requestRepository,
                               ObjectMapper objectMapper) {
        this.documentRepository = documentRepository;
        this.documentMapper = documentMapper;
        this.storageService = storageService;
        this.aiProcessor = aiProcessor;
        this.validationService = validationService;
        this.requestRepository = requestRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public DocumentResponse uploadDocument(MultipartFile file, DocumentUploadRequest uploadRequest, String userId) {
        try {
            String contentHash = storageService.computeContentHash(file);

            boolean duplicate = documentRepository.existsByContentHash(contentHash);
            if (duplicate) {
                Document existing = documentRepository.findByContentHash(contentHash).orElse(null);
                if (existing != null) {
                    log.info("Duplicate document detected: {} matches {}", file.getOriginalFilename(), existing.getOriginalName());
                    return documentMapper.toResponse(existing);
                }
            }

            String s3Key = storageService.uploadFile(file, "documents");

            Document doc = Document.builder()
                    .fileName(s3Key.substring(s3Key.lastIndexOf('/') + 1))
                    .originalName(file.getOriginalFilename())
                    .mimeType(file.getContentType())
                    .fileSize(file.getSize())
                    .processingStatus(ProcessingStatus.UPLOADED)
                    .uploadedBy(userId)
                    .uploadedAt(Instant.now())
                    .s3Bucket("")
                    .s3Key(s3Key)
                    .s3Url(storageService.getPublicUrl(s3Key))
                    .contentHash(contentHash)
                    .category(uploadRequest != null ? uploadRequest.getCategory() : null)
                    .tags(uploadRequest != null ? uploadRequest.getTags() : null)
                    .notes(uploadRequest != null ? uploadRequest.getNotes() : null)
                    .requestId(uploadRequest != null ? uploadRequest.getRequestId() : null)
                    .requestTypeId(uploadRequest != null ? uploadRequest.getRequestTypeId() : null)
                    .pageCount(1)
                    .build();

            Document saved = documentRepository.save(doc);
            log.info("Document uploaded: id={} name={} size={}", saved.getId(), saved.getOriginalName(), saved.getFileSize());

            processDocumentAsync(saved.getId());

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

    @Async("workflowExecutor")
    public void processDocumentAsync(String documentId) {
        log.info("Processing document: {}", documentId);
        Document doc = documentRepository.findById(documentId).orElse(null);
        if (doc == null) return;

        try {
            doc.setProcessingStatus(ProcessingStatus.PROCESSING);
            documentRepository.save(doc);

            byte[] fileData = storageService.downloadFile(doc.getS3Key());
            if (fileData == null) {
                doc.setProcessingStatus(ProcessingStatus.FAILED);
                documentRepository.save(doc);
                return;
            }

            DocumentAIProcessor.DocumentTypeResult typeResult = aiProcessor.classifyDocument(fileData, doc);
            doc.setDocumentType(typeResult.documentType());
            doc.setDocumentTypeConfidence(typeResult.confidence());
            doc.setProcessingStatus(ProcessingStatus.OCR_COMPLETED);
            documentRepository.save(doc);

            String ocrText = aiProcessor.performOCR(fileData);
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

            log.info("Document processed: id={} type={} status={} fields={}",
                    doc.getId(), doc.getDocumentType(), doc.getProcessingStatus(),
                    fields != null ? fields.size() : 0);

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
                                                   String search, String tag, Boolean archived) {
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
        } else if (Boolean.FALSE.equals(archived)) {
            docPage = documentRepository.findByArchivedFalse(pageable);
        } else {
            docPage = documentRepository.findAll(pageable);
        }

        return docPage.map(documentMapper::toResponse);
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
    public void deleteDocument(String id) {
        Document doc = findOrThrow(id);
        if (doc.getS3Key() != null) storageService.deleteFile(doc.getS3Key());
        documentRepository.delete(doc);
        log.info("Document deleted: {}", id);
    }

    @Override
    public byte[] getDocumentFile(String id) {
        Document doc = findOrThrow(id);
        if (doc.getS3Key() == null) return null;
        return storageService.downloadFile(doc.getS3Key());
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

    private Document findOrThrow(String id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));
    }
}
