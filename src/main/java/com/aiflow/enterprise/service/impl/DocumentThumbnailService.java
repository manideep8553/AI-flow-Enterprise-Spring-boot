package com.aiflow.enterprise.service.impl;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
public class DocumentThumbnailService {

    private static final Logger log = LoggerFactory.getLogger(DocumentThumbnailService.class);

    private static final List<String> IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp");
    private static final List<String> PDF_TYPES = List.of("application/pdf");

    private final int thumbnailWidth;
    private final int thumbnailHeight;
    private final String thumbnailFormat;

    public DocumentThumbnailService(
            @Value("${app.document.thumbnail.width:256}") int thumbnailWidth,
            @Value("${app.document.thumbnail.height:256}") int thumbnailHeight,
            @Value("${app.document.thumbnail.format:png}") String thumbnailFormat) {
        this.thumbnailWidth = thumbnailWidth;
        this.thumbnailHeight = thumbnailHeight;
        this.thumbnailFormat = thumbnailFormat;
    }

    public byte[] generateThumbnail(byte[] fileData, String mimeType, String fileName) {
        if (fileData == null || fileData.length == 0) {
            log.warn("Cannot generate thumbnail for empty file: {}", fileName);
            return null;
        }

        try {
            if (IMAGE_TYPES.contains(mimeType)) {
                return generateImageThumbnail(fileData);
            } else if (PDF_TYPES.contains(mimeType)) {
                return generatePdfThumbnail(fileData);
            } else {
                log.debug("No thumbnail support for mime type: {} file: {}", mimeType, fileName);
                return null;
            }
        } catch (Exception e) {
            log.warn("Thumbnail generation failed for {}: {}", fileName, e.getMessage());
            return null;
        }
    }

    private byte[] generateImageThumbnail(byte[] fileData) throws IOException {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(fileData));
        if (original == null) return null;

        BufferedImage thumbnail = resizeImage(original);
        return toBytes(thumbnail);
    }

    private byte[] generatePdfThumbnail(byte[] fileData) throws IOException {
        try (PDDocument document = Loader.loadPDF(fileData)) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage pageImage = renderer.renderImageWithDPI(0, 72);
            BufferedImage thumbnail = resizeImage(pageImage);
            return toBytes(thumbnail);
        } catch (Exception e) {
            log.warn("PDF thumbnail generation failed: {}", e.getMessage());
            return null;
        }
    }

    private BufferedImage resizeImage(BufferedImage original) {
        int origWidth = original.getWidth();
        int origHeight = original.getHeight();

        double scale = Math.min(
                (double) thumbnailWidth / origWidth,
                (double) thumbnailHeight / origHeight);
        int newWidth = (int) (origWidth * scale);
        int newHeight = (int) (origHeight * scale);

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(original, 0, 0, newWidth, newHeight, null);
        g.dispose();

        return resized;
    }

    private byte[] toBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, thumbnailFormat, baos);
        return baos.toByteArray();
    }
}
