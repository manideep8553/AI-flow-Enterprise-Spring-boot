package com.aiflow.enterprise.controller.v1;

import com.aiflow.enterprise.dto.request.DocumentUploadRequest;
import com.aiflow.enterprise.dto.response.ApiResponse;
import com.aiflow.enterprise.dto.response.DocumentResponse;
import com.aiflow.enterprise.entity.embedded.DocumentVersion;
import com.aiflow.enterprise.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Document Management", description = "Upload, download, version, and manage documents with S3 storage")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a document with metadata and processing options")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) String requestTypeId,
            @RequestParam(defaultValue = "true") boolean generateThumbnail,
            @RequestParam(defaultValue = "true") boolean virusScan,
            @RequestParam(defaultValue = "false") boolean enableEncryption,
            @RequestParam(required = false) String kmsKeyId,
            @RequestParam(defaultValue = "0") int retentionDays,
            @RequestParam(defaultValue = "false") boolean enableCompression,
            @RequestParam(required = false) String changeNotes,
            Authentication auth) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File is empty", HttpStatus.BAD_REQUEST));
        }

        String userId = auth != null ? auth.getName() : "anonymous";

        DocumentUploadRequest uploadRequest = DocumentUploadRequest.builder()
                .category(category).tags(tags).notes(notes)
                .requestId(requestId).requestTypeId(requestTypeId)
                .generateThumbnail(generateThumbnail).virusScan(virusScan)
                .enableEncryption(enableEncryption).kmsKeyId(kmsKeyId)
                .retentionDays(retentionDays).changeNotes(changeNotes)
                .enableCompression(enableCompression)
                .build();

        DocumentResponse response = documentService.uploadDocument(file, uploadRequest, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @GetMapping
    @Operation(summary = "List documents with optional filters")
    public ResponseEntity<ApiResponse<Page<DocumentResponse>>> listDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) String processingStatus,
            @RequestParam(required = false) String uploadedBy,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean archived) {
        Page<DocumentResponse> documents = documentService.getAllDocuments(
                page, size, documentType, processingStatus, uploadedBy,
                search, tag, category, archived);
        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document metadata by ID")
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocument(@PathVariable String id) {
        DocumentResponse doc = documentService.getDocumentById(id);
        return ResponseEntity.ok(ApiResponse.success(doc));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download document file via signed URL")
    public ResponseEntity<?> downloadDocument(@PathVariable String id) {
        String url = documentService.getDocumentDownloadUrl(id);
        if (url == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of("downloadUrl", url)));
    }

    @GetMapping("/{id}/download-url")
    @Operation(summary = "Get a signed download URL for the document")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDownloadUrl(
            @PathVariable String id,
            @RequestParam(defaultValue = "900") long expirySeconds) {
        String url = documentService.getDocumentDownloadUrl(id, expirySeconds);
        DocumentResponse doc = documentService.getDocumentById(id);
        if (url == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "downloadUrl", url,
                "fileName", doc.getOriginalName(),
                "mimeType", doc.getMimeType(),
                "fileSize", doc.getFileSize(),
                "expiresIn", expirySeconds)));
    }

    @GetMapping("/{id}/preview")
    @Operation(summary = "Get document preview URL")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPreviewUrl(@PathVariable String id) {
        String url = documentService.getDocumentPreviewUrl(id);
        DocumentResponse doc = documentService.getDocumentById(id);
        if (url == null) {
            return ResponseEntity.ok(ApiResponse.success(Map.of("previewAvailable", false)));
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "previewUrl", url,
                "previewAvailable", true,
                "pageCount", doc.getPageCount())));
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "Get document version history")
    public ResponseEntity<ApiResponse<List<DocumentVersion>>> getVersions(@PathVariable String id) {
        List<DocumentVersion> versions = documentService.getDocumentVersions(id);
        return ResponseEntity.ok(ApiResponse.success(versions));
    }

    @PostMapping("/{id}/versions/{versionNumber}/restore")
    @Operation(summary = "Restore a document to a previous version")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<DocumentResponse>> restoreVersion(
            @PathVariable String id,
            @PathVariable int versionNumber) {
        DocumentResponse response = documentService.restoreDocumentVersion(id, versionNumber);
        return ResponseEntity.ok(ApiResponse.success(response, "Restored to version " + versionNumber));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update document metadata or replace file")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<DocumentResponse>> updateDocument(
            @PathVariable String id,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String notes,
            @RequestParam(defaultValue = "0") int retentionDays,
            @RequestParam(required = false) String changeNotes,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : "anonymous";
        DocumentUploadRequest updateRequest = DocumentUploadRequest.builder()
                .category(category).tags(tags).notes(notes)
                .retentionDays(retentionDays).changeNotes(changeNotes)
                .build();
        DocumentResponse response = documentService.updateDocument(id, file, updateRequest, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a document and its S3 files")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable String id) {
        documentService.deleteDocument(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }

    @PostMapping("/bulk/delete")
    @Operation(summary = "Bulk delete multiple documents")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> bulkDelete(@RequestBody List<String> ids) {
        documentService.bulkDelete(ids);
        return ResponseEntity.ok(ApiResponse.success(ids.size() + " documents deleted"));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive a document")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<DocumentResponse>> archiveDocument(@PathVariable String id) {
        DocumentResponse response = documentService.archiveDocument(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Document archived"));
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore an archived document")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<DocumentResponse>> restoreDocument(@PathVariable String id) {
        DocumentResponse response = documentService.restoreDocument(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Document restored"));
    }

    @PostMapping("/{id}/reprocess")
    @Operation(summary = "Re-run AI processing pipeline on a document")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DocumentResponse>> reprocessDocument(@PathVariable String id) {
        DocumentResponse response = documentService.reprocessDocument(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Reprocessing initiated"));
    }

    @PostMapping("/upload-url")
    @Operation(summary = "Get a signed URL for direct browser-to-S3 upload")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUploadUrl(
            @RequestParam String fileName,
            @RequestParam String contentType) {
        Map<String, Object> urlInfo = documentService.getDocumentUploadUrl(fileName, contentType);
        return ResponseEntity.ok(ApiResponse.success(urlInfo));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get document statistics and counts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStatistics() {
        Map<String, Long> stats = documentService.getDocumentStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
