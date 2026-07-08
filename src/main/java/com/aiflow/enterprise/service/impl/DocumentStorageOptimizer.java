package com.aiflow.enterprise.service.impl;

import com.aiflow.enterprise.entity.Document;
import com.aiflow.enterprise.entity.embedded.StorageInfo;
import com.aiflow.enterprise.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Service
public class DocumentStorageOptimizer {

    private static final Logger log = LoggerFactory.getLogger(DocumentStorageOptimizer.class);

    private static final List<String> COMPRESSIBLE_TYPES = List.of(
            "text/plain", "text/html", "text/csv", "text/xml", "application/json",
            "application/xml", "application/javascript", "text/css",
            "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final long compressionThresholdBytes;
    private final DocumentRepository documentRepository;

    public DocumentStorageOptimizer(
            @Value("${app.document.storage.compression-threshold:4096}") long compressionThresholdBytes,
            DocumentRepository documentRepository) {
        this.compressionThresholdBytes = compressionThresholdBytes;
        this.documentRepository = documentRepository;
    }

    public byte[] maybeCompress(byte[] fileData, String mimeType, String fileName) {
        if (fileData == null || fileData.length < compressionThresholdBytes) {
            return fileData;
        }

        if (!COMPRESSIBLE_TYPES.contains(mimeType) && !isCompressibleFileName(fileName)) {
            return fileData;
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
                gzip.write(fileData);
            }
            byte[] compressed = baos.toByteArray();

            if (compressed.length < fileData.length) {
                log.debug("Compressed {} from {} to {} bytes (ratio: {})",
                        fileName, fileData.length, compressed.length,
                        String.format("%.2f", (double) compressed.length / fileData.length));
                return compressed;
            }
            return fileData;
        } catch (IOException e) {
            log.warn("Compression failed for {}: {}", fileName, e.getMessage());
            return fileData;
        }
    }

    public byte[] maybeDecompress(byte[] data, String storageClass, String compressionAlgorithm) {
        if (compressionAlgorithm == null || !"gzip".equals(compressionAlgorithm)) {
            return data;
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data))) {
                gzip.transferTo(baos);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            log.warn("Decompression failed: {}", e.getMessage());
            return data;
        }
    }

    public StorageInfo analyzeStorage(Document doc, byte[] originalData, byte[] storedData) {
        long savings = originalData.length - storedData.length;
        double ratio = storedData.length > 0 ? (double) storedData.length / originalData.length : 1.0;

        return StorageInfo.builder()
                .storageClass(determineStorageClass(doc))
                .originalSize(originalData.length)
                .compressedSize(storedData.length)
                .compressionAlgorithm(storedData.length < originalData.length ? "gzip" : null)
                .compressionRatio(Math.round((1.0 - ratio) * 10000.0) / 100.0)
                .optimizedAt(Instant.now())
                .optimizationAction(storedData.length < originalData.length ? "compressed" : "none")
                .storageSavingsBytes(savings > 0 ? savings : 0)
                .build();
    }

    public String determineStorageClass(Document doc) {
        if (doc.getLifecyclePolicy() != null && doc.getLifecyclePolicy().getStorageClass() != null) {
            return doc.getLifecyclePolicy().getStorageClass();
        }
        Instant lastAccess = doc.getUpdatedAt() != null ? doc.getUpdatedAt() : doc.getCreatedAt();
        if (lastAccess == null) return "STANDARD";

        long daysSinceAccess = ChronoUnit.DAYS.between(lastAccess, Instant.now());
        if (daysSinceAccess > 365) return "DEEP_ARCHIVE";
        if (daysSinceAccess > 90) return "STANDARD_IA";
        if (daysSinceAccess > 30) return "STANDARD";
        return "STANDARD";
    }

    public int optimizeStorage() {
        int optimized = 0;
        List<Document> docs = documentRepository.findAll();

        for (Document doc : docs) {
            try {
                String currentClass = doc.getStorageInfo() != null
                        ? doc.getStorageInfo().getStorageClass() : "STANDARD";
                String targetClass = determineStorageClass(doc);

                if (!currentClass.equals(targetClass)) {
                    if (doc.getStorageInfo() == null) {
                        doc.setStorageInfo(StorageInfo.builder().build());
                    }
                    doc.getStorageInfo().setStorageClass(targetClass);
                    doc.getStorageInfo().setOptimizedAt(Instant.now());
                    doc.getStorageInfo().setOptimizationAction("storage_class_change");
                    documentRepository.save(doc);
                    optimized++;
                }
            } catch (Exception e) {
                log.warn("Failed to optimize storage for document {}: {}", doc.getId(), e.getMessage());
            }
        }

        log.info("Storage optimization complete: {} documents updated", optimized);
        return optimized;
    }

    private boolean isCompressibleFileName(String fileName) {
        if (fileName == null) return false;
        String lower = fileName.toLowerCase();
        return lower.endsWith(".txt") || lower.endsWith(".csv") || lower.endsWith(".json")
                || lower.endsWith(".xml") || lower.endsWith(".html") || lower.endsWith(".log");
    }
}
