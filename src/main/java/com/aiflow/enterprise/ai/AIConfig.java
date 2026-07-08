package com.aiflow.enterprise.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.model:gpt-4o-mini}")
    private String model;

    private final ChatClient.Builder chatClientBuilder;

    public AIConfig(ChatClient.Builder chatClientBuilder) {
        this.chatClientBuilder = chatClientBuilder;
    }

    @Bean
    public ChatClient chatClient() {
        return chatClientBuilder
                .defaultSystem("You are an AI workflow assistant. Provide helpful, concise responses.")
                .build();
    }
}
