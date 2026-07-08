package com.aiflow.enterprise.service.impl;

import com.aiflow.enterprise.entity.embedded.DocumentValidationResult;
import com.aiflow.enterprise.entity.embedded.ExtractedField;
import com.aiflow.enterprise.enums.DocumentType;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentValidationService {

    public List<DocumentValidationResult> validate(DocumentType docType, List<ExtractedField> fields) {
        List<DocumentValidationResult> results = new ArrayList<>();

        for (ExtractedField field : fields) {
            if (field.getValue() == null || field.getValue().isBlank()) {
                results.add(DocumentValidationResult.builder()
                        .ruleName("required_field")
                        .passed(false)
                        .severity("WARNING")
                        .message("Missing required field: " + field.getLabel())
                        .fieldName(field.getName())
                        .build());
                continue;
            }

            switch (field.getDataType() != null ? field.getDataType() : "string") {
                case "number", "currency", "percentage" -> validateNumeric(field, results);
                case "date" -> validateDate(field, results);
                case "email" -> validateEmail(field, results);
                case "phone" -> validatePhone(field, results);
                default -> {}
            }

            if (field.getConfidence() < 0.5) {
                results.add(DocumentValidationResult.builder()
                        .ruleName("low_confidence")
                        .passed(true)
                        .severity("WARNING")
                        .message("Low confidence extraction for " + field.getLabel()
                                + ": " + String.format("%.2f", field.getConfidence()))
                        .fieldName(field.getName())
                        .actualValue(String.valueOf(field.getConfidence()))
                        .build());
            }
        }

        validateDocumentSpecific(docType, fields, results);
        return results;
    }

    private void validateNumeric(ExtractedField field, List<DocumentValidationResult> results) {
        try {
            double val = Double.parseDouble(field.getNormalizedValue());
            if (val < 0) {
                results.add(DocumentValidationResult.builder()
                        .ruleName("negative_value")
                        .passed(false)
                        .severity("ERROR")
                        .message(field.getLabel() + " has a negative value: " + val)
                        .fieldName(field.getName())
                        .actualValue(String.valueOf(val))
                        .build());
            }
            if (val > 999999999.99) {
                results.add(DocumentValidationResult.builder()
                        .ruleName("unusually_large")
                        .passed(true)
                        .severity("WARNING")
                        .message(field.getLabel() + " is unusually large: " + val)
                        .fieldName(field.getName())
                        .actualValue(String.valueOf(val))
                        .build());
            }
        } catch (NumberFormatException e) {
            results.add(DocumentValidationResult.builder()
                    .ruleName("invalid_number")
                    .passed(false)
                    .severity("ERROR")
                    .message(field.getLabel() + " is not a valid number: " + field.getValue())
                    .fieldName(field.getName())
                    .actualValue(field.getValue())
                    .build());
        }
    }

    private void validateDate(ExtractedField field, List<DocumentValidationResult> results) {
        try {
            Instant.parse(field.getNormalizedValue());
        } catch (DateTimeParseException e) {
            try {
                DateTimeFormatter.ofPattern("yyyy-MM-dd").parse(field.getValue());
            } catch (DateTimeParseException e2) {
                results.add(DocumentValidationResult.builder()
                        .ruleName("invalid_date")
                        .passed(false)
                        .severity("WARNING")
                        .message(field.getLabel() + " is not a valid date: " + field.getValue())
                        .fieldName(field.getName())
                        .actualValue(field.getValue())
                        .build());
            }
        }
    }

    private void validateEmail(ExtractedField field, List<DocumentValidationResult> results) {
        String email = field.getValue();
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            results.add(DocumentValidationResult.builder()
                    .ruleName("invalid_email")
                    .passed(false)
                    .severity("WARNING")
                    .message(field.getLabel() + " is not a valid email: " + email)
                    .fieldName(field.getName())
                    .actualValue(email)
                    .build());
        }
    }

    private void validatePhone(ExtractedField field, List<DocumentValidationResult> results) {
        String phone = field.getValue().replaceAll("[^0-9+]", "");
        if (phone.length() < 7 || phone.length() > 15) {
            results.add(DocumentValidationResult.builder()
                    .ruleName("invalid_phone")
                    .passed(false)
                    .severity("WARNING")
                    .message(field.getLabel() + " is not a valid phone number: " + field.getValue())
                    .fieldName(field.getName())
                    .actualValue(field.getValue())
                    .build());
        }
    }

    private void validateDocumentSpecific(DocumentType docType, List<ExtractedField> fields,
                                           List<DocumentValidationResult> results) {
        boolean hasTotal = false;
        boolean hasSubtotal = false;
        boolean hasTax = false;
        boolean hasNumber = false;
        boolean hasDate = false;
        boolean hasVendor = false;

        for (ExtractedField f : fields) {
            String name = f.getName().toLowerCase();
            if (name.contains("total")) hasTotal = true;
            if (name.contains("subtotal") || name.contains("sub_total")) hasSubtotal = true;
            if (name.contains("tax") || name.contains("vat") || name.contains("gst")) hasTax = true;
            if (name.contains("number") || name.contains("id") || name.contains("reference")) hasNumber = true;
            if (name.contains("date")) hasDate = true;
            if (name.contains("vendor") || name.contains("supplier") || name.contains("merchant") || name.contains("from")) hasVendor = true;
        }

        if (docType == DocumentType.INVOICE || docType == DocumentType.BILL) {
            if (!hasTotal) results.add(missingField("WARNING", "Total amount", "Standard for " + docType));
            if (!hasNumber) results.add(missingField("WARNING", "Document number", "Standard for " + docType));
            if (!hasDate) results.add(missingField("WARNING", "Document date", "Standard for " + docType));
            if (!hasVendor) results.add(missingField("WARNING", "Vendor/Supplier name", "Standard for " + docType));
            if (!hasTax) results.add(missingField("INFO", "Tax amount", "May not apply"));
        }

        if (docType == DocumentType.PURCHASE_ORDER) {
            if (!hasNumber) results.add(missingField("WARNING", "PO number", "Standard for purchase orders"));
            if (!hasDate) results.add(missingField("WARNING", "Order date", "Standard for purchase orders"));
            if (!hasVendor) results.add(missingField("WARNING", "Vendor name", "Standard for purchase orders"));
        }

        if (docType == DocumentType.CONTRACT) {
            if (!hasDate) results.add(missingField("WARNING", "Effective/expiration date", "Standard for contracts"));
            if (!hasVendor) results.add(missingField("WARNING", "Party name", "Standard for contracts"));
        }
    }

    private DocumentValidationResult missingField(String severity, String fieldLabel, String reason) {
        return DocumentValidationResult.builder()
                .ruleName("missing_standard_field")
                .passed(true)
                .severity(severity)
                .message("Could not extract: " + fieldLabel + ". " + reason)
                .build();
    }
}
