package com.aiflow.enterprise.service.impl;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DocumentPreviewService {

    private static final Logger log = LoggerFactory.getLogger(DocumentPreviewService.class);

    private final int previewMaxPages;
    private final int previewDpi;
    private final String previewFormat;

    public DocumentPreviewService(
            @Value("${app.document.preview.max-pages:5}") int previewMaxPages,
            @Value("${app.document.preview.dpi:150}") int previewDpi,
            @Value("${app.document.preview.format:png}") String previewFormat) {
        this.previewMaxPages = previewMaxPages;
        this.previewDpi = previewDpi;
        this.previewFormat = previewFormat;
    }

    public PreviewResult generatePreview(byte[] fileData, String mimeType, String fileName) {
        if (fileData == null || fileData.length == 0) {
            return new PreviewResult(List.of(), "empty", 0);
        }

        if ("application/pdf".equals(mimeType)) {
            return generatePdfPreview(fileData);
        }

        return new PreviewResult(List.of(), "unsupported", 0);
    }

    private PreviewResult generatePdfPreview(byte[] fileData) {
        try (PDDocument document = Loader.loadPDF(fileData)) {
            int totalPages = document.getNumberOfPages();
            int pagesToRender = Math.min(totalPages, previewMaxPages);
            PDFRenderer renderer = new PDFRenderer(document);

            List<Map<String, Object>> pages = new ArrayList<>();
            for (int i = 0; i < pagesToRender; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, previewDpi);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, previewFormat, baos);

                Map<String, Object> page = new HashMap<>();
                page.put("pageNumber", i + 1);
                page.put("width", image.getWidth());
                page.put("height", image.getHeight());
                page.put("data", baos.toByteArray());
                pages.add(page);
            }

            log.info("Generated preview for PDF: {} pages rendered out of {}", pagesToRender, totalPages);
            return new PreviewResult(pages, "pdf", totalPages);
        } catch (Exception e) {
            log.warn("PDF preview generation failed: {}", e.getMessage());
            return new PreviewResult(List.of(), "error", 0);
        }
    }

    public record PreviewResult(List<Map<String, Object>> pages, String format, int totalPages) {}
}
