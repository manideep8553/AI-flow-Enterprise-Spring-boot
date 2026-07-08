package com.aiflow.enterprise.service;

import com.aiflow.enterprise.dto.request.DocumentUploadRequest;
import com.aiflow.enterprise.dto.response.DocumentResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {

    DocumentResponse uploadDocument(MultipartFile file, DocumentUploadRequest uploadRequest, String userId);

    DocumentResponse getDocumentById(String id);

    Page<DocumentResponse> getAllDocuments(int page, int size, String documentType,
                                            String processingStatus, String uploadedBy,
                                            String search, String tag, Boolean archived);

    DocumentResponse reprocessDocument(String id);

    void deleteDocument(String id);

    byte[] getDocumentFile(String id);

    long getDocumentCountByType(String documentType);

    long getDocumentCountByStatus(String processingStatus);
}
