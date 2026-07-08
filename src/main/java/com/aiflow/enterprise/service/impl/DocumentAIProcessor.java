package com.aiflow.enterprise.service.impl;

import com.aiflow.enterprise.entity.Document;
import com.aiflow.enterprise.entity.Request;
import com.aiflow.enterprise.entity.embedded.AnomalyResult;
import com.aiflow.enterprise.entity.embedded.DuplicateInfo;
import com.aiflow.enterprise.entity.embedded.ExtractedField;
import com.aiflow.enterprise.enums.DocumentType;
import com.aiflow.enterprise.repository.DocumentRepository;
import com.aiflow.enterprise.repository.RequestRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.Block;
import software.amazon.awssdk.services.textract.model.DetectDocumentTextRequest;
import software.amazon.awssdk.services.textract.model.DetectDocumentTextResponse;
import software.amazon.awssdk.services.textract.model.FeatureType;
import software.amazon.awssdk.services.textract.model.S3Object;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DocumentAIProcessor {

    private static final Logger log = LoggerFactory.getLogger(DocumentAIProcessor.class);

    private final TextractClient textractClient;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final DocumentRepository documentRepository;
    private final RequestRepository requestRepository;
    private final DocumentStorageService storageService;

    public DocumentAIProcessor(TextractClient textractClient,
                               ChatClient chatClient,
                               ObjectMapper objectMapper,
                               DocumentRepository documentRepository,
                               RequestRepository requestRepository,
                               DocumentStorageService storageService) {
        this.textractClient = textractClient;
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
        this.documentRepository = documentRepository;
        this.requestRepository = requestRepository;
        this.storageService = storageService;
    }

    public DocumentTypeResult classifyDocument(byte[] imageData, Document doc) {
        try {
            String base64 = java.util.Base64.getEncoder().encodeToString(imageData);
            String prompt = "Analyze this document image and determine its type. "
                    + "Return JSON with: documentType (one of: INVOICE, RECEIPT, BILL, CONTRACT, "
                    + "PURCHASE_ORDER, CERTIFICATE, ID_PROOF, BANK_STATEMENT, TAX_FORM, REPORT, LETTER, FORM, OTHER), "
                    + "confidence (0.0-1.0), reasoning, and keyVisualFeatures (list).";

            String response = chatClient.prompt()
                    .system("You are a document classification AI. Identify document types from images.")
                    .user(prompt)
                    .call()
                    .content();

            if (response == null) response = "{}";
            Map<String, Object> parsed = parseJson(response);

            String typeStr = getString(parsed, "documentType", "OTHER");
            DocumentType type;
            try { type = DocumentType.valueOf(typeStr); }
            catch (IllegalArgumentException e) { type = DocumentType.OTHER; }

            double confidence = getDouble(parsed, "confidence", 0.5);
            String reasoning = getString(parsed, "reasoning", "");

            return new DocumentTypeResult(type, confidence, reasoning);
        } catch (Exception e) {
            log.warn("AI document classification failed: {}", e.getMessage());
            return new DocumentTypeResult(DocumentType.OTHER, 0.3, "Classification error: " + e.getMessage());
        }
    }

    public String performOCR(byte[] imageData) {
        try {
            DetectDocumentTextRequest request = DetectDocumentTextRequest.builder()
                    .document(software.amazon.awssdk.services.textract.model.Document.builder()
                            .bytes(SdkBytes.fromByteArray(imageData))
                            .build())
                    .build();
            DetectDocumentTextResponse response = textractClient.detectDocumentText(request);
            StringBuilder text = new StringBuilder();
            for (Block block : response.blocks()) {
                if ("LINE".equals(block.blockTypeAsString())) {
                    text.append(block.text()).append("\n");
                }
            }
            log.info("OCR completed: {} lines extracted", text.toString().split("\n").length);
            return text.toString();
        } catch (Exception e) {
            log.error("OCR failed: {}", e.getMessage());
            return "OCR processing failed: " + e.getMessage();
        }
    }

    public List<ExtractedField> extractFields(DocumentType docType, String ocrText, Document doc) {
        try {
            String prompt = buildExtractionPrompt(docType, ocrText, doc);

            String response = chatClient.prompt()
                    .system("You are a document data extraction AI. Extract structured fields from OCR text. "
                            + "Return only valid JSON with an 'extractedData' map and 'fields' array. "
                            + "Each field must have: name, label, value, normalizedValue, confidence (0.0-1.0), source.")
                    .user(prompt)
                    .call()
                    .content();

            if (response == null) response = "{}";
            Map<String, Object> parsed = parseJson(response);

            List<ExtractedField> fields = new ArrayList<>();
            Object fieldsObj = parsed.get("fields");
            if (fieldsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> rawFields = (List<Map<String, Object>>) fieldsObj;
                for (Map<String, Object> raw : rawFields) {
                    fields.add(ExtractedField.builder()
                            .name(getString(raw, "name", ""))
                            .label(getString(raw, "label", ""))
                            .value(getString(raw, "value", ""))
                            .normalizedValue(getString(raw, "normalizedValue", ""))
                            .confidence(getDouble(raw, "confidence", 0.5))
                            .source(getString(raw, "source", "AI"))
                            .dataType(getString(raw, "dataType", "string"))
                            .build());
                }
            }

            return fields;

        } catch (Exception e) {
            log.warn("AI field extraction failed: {}", e.getMessage());
            return List.of();
        }
    }

    public String generateSummary(DocumentType docType, List<ExtractedField> fields, String ocrText) {
        try {
            String prompt = "Summarize this " + docType + " document:\n\n"
                    + "OCR Text:\n" + (ocrText.length() > 2000 ? ocrText.substring(0, 2000) : ocrText) + "\n\n"
                    + "Extracted Fields:\n";
            for (ExtractedField f : fields) {
                prompt += "- " + f.getLabel() + ": " + f.getValue()
                        + " (confidence: " + String.format("%.2f", f.getConfidence()) + ")\n";
            }
            prompt += "\nProvide a concise 2-3 sentence summary of this document.";

            String response = chatClient.prompt()
                    .system("You are a document summarizer. Create concise summaries.")
                    .user(prompt)
                    .call()
                    .content();

            return response != null ? response : "Summary not available";
        } catch (Exception e) {
            log.warn("AI summary generation failed: {}", e.getMessage());
            return "Summary generation failed";
        }
    }

    public String analyzeDocument(String ocrText, DocumentType docType) {
        try {
            String prompt = "Analyze this " + docType + " document OCR text for anomalies, "
                    + "missing information, or inconsistencies:\n\n"
                    + (ocrText.length() > 3000 ? ocrText.substring(0, 3000) : ocrText)
                    + "\n\nReturn JSON with: analysis (string), anomalies (array of {type, description, severity}), "
                    + "completeness (0.0-1.0), suggestions (list).";

            String response = chatClient.prompt()
                    .system("You are a document analysis AI. Identify issues and provide insights.")
                    .user(prompt)
                    .call()
                    .content();

            return response != null ? response : "{}";
        } catch (Exception e) {
            log.warn("AI analysis failed: {}", e.getMessage());
            return "{}";
        }
    }

    public List<AnomalyResult> detectAnomalies(Document doc, List<ExtractedField> fields) {
        List<AnomalyResult> anomalies = new ArrayList<>();

        try {
            Map<String, Object> fieldMap = new HashMap<>();
            for (ExtractedField f : fields) {
                fieldMap.put(f.getName(), f.getValue());
            }

            String prompt = "Detect anomalies in this " + doc.getDocumentType() + " document:\n\n"
                    + "Fields: " + objectMapper.writeValueAsString(fieldMap) + "\n\n"
                    + "Return JSON with anomalies array. Each anomaly has: type, description, "
                    + "severityScore (0.0-1.0), fieldName, expectedValue, actualValue, recommendation.";

            String response = chatClient.prompt()
                    .system("You are an anomaly detection AI for business documents.")
                    .user(prompt)
                    .call()
                    .content();

            if (response != null) {
                Map<String, Object> parsed = parseJson(response);
                Object anomaliesObj = parsed.get("anomalies");
                if (anomaliesObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> raw = (List<Map<String, Object>>) anomaliesObj;
                    for (Map<String, Object> r : raw) {
                        anomalies.add(AnomalyResult.builder()
                                .type(getString(r, "type", "UNKNOWN"))
                                .description(getString(r, "description", ""))
                                .severityScore(getDouble(r, "severityScore", 0.5))
                                .fieldName(getString(r, "fieldName", ""))
                                .expectedValue(getString(r, "expectedValue", ""))
                                .actualValue(getString(r, "actualValue", ""))
                                .recommendation(getString(r, "recommendation", ""))
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Anomaly detection failed: {}", e.getMessage());
        }

        return anomalies;
    }

    public DuplicateInfo checkDuplicate(Document doc, List<ExtractedField> fields) {
        DuplicateInfo result = DuplicateInfo.builder().isDuplicate(false).matchScore(0.0).build();

        try {
            String docNumber = null;
            String vendorName = null;
            Double totalAmount = null;
            String dateStr = null;

            for (ExtractedField f : fields) {
                String name = f.getName().toLowerCase();
                if (name.contains("invoice") || name.contains("number") || name.contains("id")) {
                    if (name.contains("number") || name.contains("invoice")) docNumber = f.getNormalizedValue();
                }
                if (name.contains("vendor") || name.contains("supplier") || name.contains("from")) vendorName = f.getValue();
                if (name.contains("total") || name.contains("amount")) {
                    try { totalAmount = Double.parseDouble(f.getNormalizedValue()); } catch (Exception ignored) {}
                }
                if (name.contains("date")) dateStr = f.getNormalizedValue();
            }

            if (docNumber != null && !docNumber.isBlank()) {
                List<Document> existing = documentRepository.findByContentHashIn(List.of());
                // Check via content hash
                if (doc.getContentHash() != null) {
                    boolean hashExists = documentRepository.existsByContentHash(doc.getContentHash());
                    if (hashExists) {
                        Document matched = documentRepository.findByContentHash(doc.getContentHash()).orElse(null);
                        if (matched != null) {
                            return DuplicateInfo.builder()
                                    .isDuplicate(true).matchScore(0.99)
                                    .matchedDocumentId(matched.getId())
                                    .matchedDocumentName(matched.getOriginalName())
                                    .matchReason("Exact content match (SHA-256)")
                                    .matchedField("contentHash")
                                    .matchedValue(doc.getContentHash())
                                    .build();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Duplicate detection failed: {}", e.getMessage());
        }

        return result;
    }

    public Map<String, Object> autoFillRequestForm(Document doc, List<ExtractedField> fields, String requestTypeId) {
        Map<String, Object> formData = new HashMap<>();
        try {
            String prompt = "Map these extracted document fields to a request form of type " + requestTypeId + ":\n\n";
            for (ExtractedField f : fields) {
                prompt += "- " + f.getName() + " (" + f.getLabel() + "): " + f.getValue() + "\n";
            }
            prompt += "\nReturn JSON mapping of form field names to extracted values.";

            String response = chatClient.prompt()
                    .system("You are a form auto-fill AI. Map document fields to form fields.")
                    .user(prompt)
                    .call()
                    .content();

            if (response != null) {
                Map<String, Object> parsed = parseJson(response);
                formData.putAll(parsed);
            }
        } catch (Exception e) {
            log.warn("Auto-fill failed: {}", e.getMessage());
        }
        return formData;
    }

    private String buildExtractionPrompt(DocumentType docType, String ocrText, Document doc) {
        String typeSpecificFields = switch (docType) {
            case INVOICE -> "invoiceNumber, vendorName, vendorAddress, billTo, shipTo, invoiceDate, dueDate, "
                    + "poNumber, subtotal, taxAmount, totalAmount, currency, paymentTerms, lineItems";
            case RECEIPT -> "receiptNumber, merchantName, merchantAddress, receiptDate, totalAmount, "
                    + "taxAmount, paymentMethod, items, currency, cardLastFour";
            case BILL -> "billNumber, providerName, providerAddress, billDate, dueDate, totalAmount, "
                    + "accountNumber, servicePeriod, charges, taxAmount";
            case CONTRACT -> "contractTitle, parties, effectiveDate, expirationDate, contractValue, "
                    + "governingLaw, renewalTerms, terminationClause, signatures";
            case PURCHASE_ORDER -> "poNumber, vendorName, orderDate, deliveryDate, shipToAddress, "
                    + "items, quantities, unitPrices, totalAmount, currency, paymentTerms";
            case CERTIFICATE -> "certificateType, issuerName, recipientName, issueDate, expiryDate, "
                    + "certificateNumber, status, governingBody";
            case ID_PROOF -> "documentType, fullName, dateOfBirth, documentNumber, issueDate, expiryDate, issuingAuthority";
            case BANK_STATEMENT -> "bankName, accountNumber, accountHolder, statementPeriod, openingBalance, "
                    + "closingBalance, transactions, totalDeposits, totalWithdrawals";
            default -> "documentReference, issuingParty, receivingParty, date, amount, description, referenceNumber";
        };

        return "Extract structured data from this " + docType + " document.\n\n"
                + "OCR Text:\n" + (ocrText.length() > 3000 ? ocrText.substring(0, 3000) : ocrText) + "\n\n"
                + "Extract these fields if present: " + typeSpecificFields + "\n"
                + "File name: " + doc.getOriginalName() + "\n"
                + "File size: " + doc.getFileSize() + " bytes\n\n"
                + "Return JSON with 'fields' array and 'extractedData' map.";
    }

    private Map<String, Object> parseJson(String text) {
        try {
            int start = text.indexOf('{');
            int end = text.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return objectMapper.readValue(text.substring(start, end + 1),
                        new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.warn("Failed to parse JSON: {}", e.getMessage());
        }
        return new HashMap<>();
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    private double getDouble(Map<String, Object> map, String key, double defaultValue) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        if (val instanceof String) { try { return Double.parseDouble((String) val); } catch (Exception e) { return defaultValue; } }
        return defaultValue;
    }

    public record DocumentTypeResult(DocumentType documentType, double confidence, String reasoning) {}
}
