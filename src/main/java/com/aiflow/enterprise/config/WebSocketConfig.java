package com.aiflow.enterprise.config;

import com.aiflow.enterprise.websocket.ExecutionMonitorHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ExecutionMonitorHandler executionHandler;

    public WebSocketConfig(ExecutionMonitorHandler executionHandler) {
        this.executionHandler = executionHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(executionHandler, "/ws/executions/{executionId}")
                .setAllowedOriginPatterns("*");
    }
}
