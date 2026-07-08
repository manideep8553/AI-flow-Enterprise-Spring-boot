package com.aiflow.enterprise.notification.entity.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalizedContent {
    private String locale;
    private String subject;
    private String body;
    private String htmlBody;
    private String pushTitle;
    private String pushBody;
    private String smsBody;
}
