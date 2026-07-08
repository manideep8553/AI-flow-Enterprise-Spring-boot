package com.aiflow.enterprise.notification.entity.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelConfig {
    private String channel;
    private boolean enabled;
    private String webhookUrl;
    private String apiKey;
    private String fromAddress;
    private Map<String, Object> channelMetadata;
}
