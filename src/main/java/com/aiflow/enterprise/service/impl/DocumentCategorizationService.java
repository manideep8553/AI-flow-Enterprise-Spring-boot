package com.aiflow.enterprise.service.impl;

import com.aiflow.enterprise.entity.Document;
import com.aiflow.enterprise.entity.embedded.ExtractedField;
import com.aiflow.enterprise.enums.DocumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DocumentCategorizationService {

    private static final Logger log = LoggerFactory.getLogger(DocumentCategorizationService.class);

    private static final Map<String, List<String>> CATEGORY_PATTERNS = new HashMap<>();

    static {
        CATEGORY_PATTERNS.put("Financial", List.of(
                "invoice", "receipt", "bill", "payment", "transaction", "bank",
                "statement", "tax", "expense", "reimbursement", "purchase order"));
        CATEGORY_PATTERNS.put("Legal", List.of(
                "contract", "agreement", "non-disclosure", "nda", "terms",
                "license", "policy", "compliance", "regulation", "legal"));
        CATEGORY_PATTERNS.put("Human Resources", List.of(
                "resume", "cv", "application", "offer letter", "employment",
                "review", "timesheet", "leave", "attendance", "onboarding"));
        CATEGORY_PATTERNS.put("Administrative", List.of(
                "form", "report", "memo", "minutes", "agenda", "notice",
                "request", "approval", "correspondence", "letter"));
        CATEGORY_PATTERNS.put("Technical", List.of(
                "specification", "technical", "architecture", "design", "api",
                "documentation", "manual", "guide", "readme", "diagram"));
        CATEGORY_PATTERNS.put("Identity", List.of(
                "passport", "driver license", "id card", "identification",
                "visa", "certificate", "proof", "verification"));
    }

    public CategorizationResult categorize(Document doc, List<ExtractedField> fields, String ocrText) {
        String text = buildSearchText(doc, fields, ocrText);
        if (text == null || text.isBlank()) {
            return new CategorizationResult(doc.getDocumentType() != null
                    ? doc.getDocumentType().name() : "Other", 0.5, List.of(), "No text available");
        }

        text = text.toLowerCase();
        Map<String, Double> scores = new HashMap<>();

        for (var entry : CATEGORY_PATTERNS.entrySet()) {
            String category = entry.getKey();
            double score = 0;
            int matchCount = 0;

            for (String pattern : entry.getValue()) {
                int count = countOccurrences(text, pattern);
                if (count > 0) {
                    score += count * (1.0 / entry.getValue().size());
                    matchCount += count;
                }
            }

            if (matchCount > 0) {
                score = Math.min(score, 1.0);
                scores.put(category, score);
            }
        }

        if (doc.getDocumentType() != null) {
            String typeCategory = mapDocumentTypeToCategory(doc.getDocumentType());
            scores.merge(typeCategory, 0.3, Double::sum);
        }

        String bestCategory = "Other";
        double bestScore = 0;

        for (var entry : scores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                bestCategory = entry.getKey();
            }
        }

        List<String> suggestedTags = new ArrayList<>();
        suggestedTags.add(bestCategory.toLowerCase().replace(' ', '-'));
        if (doc.getDocumentType() != null) {
            suggestedTags.add(doc.getDocumentType().name().toLowerCase());
        }

        log.info("Categorized document {} as '{}' with confidence {}", doc.getId(), bestCategory,
                String.format("%.2f", bestScore));
        return new CategorizationResult(bestCategory, Math.min(bestScore, 1.0), suggestedTags,
                "Auto-categorized based on content analysis");
    }

    private String buildSearchText(Document doc, List<ExtractedField> fields, String ocrText) {
        StringBuilder sb = new StringBuilder();
        if (doc.getOriginalName() != null) sb.append(doc.getOriginalName()).append(" ");
        if (doc.getFileName() != null) sb.append(doc.getFileName()).append(" ");
        if (ocrText != null) sb.append(ocrText).append(" ");
        if (fields != null) {
            for (ExtractedField f : fields) {
                if (f.getValue() != null) sb.append(f.getValue()).append(" ");
                if (f.getLabel() != null) sb.append(f.getLabel()).append(" ");
            }
        }
        if (doc.getSummary() != null) sb.append(doc.getSummary());
        return sb.toString();
    }

    private String mapDocumentTypeToCategory(DocumentType type) {
        return switch (type) {
            case INVOICE, RECEIPT, BILL, PURCHASE_ORDER, BANK_STATEMENT, TAX_FORM -> "Financial";
            case CONTRACT, CERTIFICATE -> "Legal";
            case ID_PROOF -> "Identity";
            case REPORT, LETTER, FORM -> "Administrative";
            default -> "Other";
        };
    }

    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(pattern, idx)) != -1) {
            count++;
            idx += pattern.length();
        }
        return count;
    }

    public record CategorizationResult(String category, double confidence,
                                        List<String> suggestedTags, String explanation) {}
}
