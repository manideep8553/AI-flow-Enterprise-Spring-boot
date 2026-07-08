package com.aiflow.enterprise.entity.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentEntry {
    private String id;
    private String author;
    private String authorName;
    private String text;
    private List<FileAttachment> attachments;
    private Instant createdAt;
    private Instant editedAt;
    private boolean internal;
}
