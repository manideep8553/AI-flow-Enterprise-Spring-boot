package com.aiflow.enterprise.service;

import com.aiflow.enterprise.dto.request.DocumentUploadRequest;
import com.aiflow.enterprise.dto.response.DocumentResponse;
import com.aiflow.enterprise.entity.Document;
import com.aiflow.enterprise.entity.embedded.DocumentVersion;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface DocumentService {

    DocumentResponse uploadDocument(MultipartFile file, DocumentUploadRequest uploadRequest, String userId);

    DocumentResponse updateDocument(String id, MultipartFile file, DocumentUploadRequest updateRequest, String userId);

    DocumentResponse getDocumentById(String id);

    Page<DocumentResponse> getAllDocuments(int page, int size, String documentType,
                                            String processingStatus, String uploadedBy,
                                            String search, String tag, String category, Boolean archived);

    DocumentResponse reprocessDocument(String id);

    DocumentResponse archiveDocument(String id);

    DocumentResponse restoreDocument(String id);

    void deleteDocument(String id);

    void bulkDelete(List<String> ids);

    byte[] getDocumentFile(String id);

    String getDocumentDownloadUrl(String id);

    String getDocumentDownloadUrl(String id, long expirySeconds);

    String getDocumentPreviewUrl(String id);

    Map<String, Object> getDocumentUploadUrl(String fileName, String contentType);

    List<DocumentVersion> getDocumentVersions(String id);

    DocumentResponse restoreDocumentVersion(String id, int versionNumber);

    long getDocumentCountByType(String documentType);

    long getDocumentCountByStatus(String processingStatus);

    Map<String, Long> getDocumentStatistics();
}
