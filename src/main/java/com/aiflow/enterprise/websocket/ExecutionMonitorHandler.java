package com.aiflow.enterprise.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class ExecutionMonitorHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ExecutionMonitorHandler.class);

    private final Map<String, CopyOnWriteArraySet<WebSocketSession>> executionSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String executionId = extractExecutionId(session);
        if (executionId != null) {
            executionSessions.computeIfAbsent(executionId, k -> new CopyOnWriteArraySet<>()).add(session);
            log.debug("WebSocket connected for execution: {}", executionId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String executionId = extractExecutionId(session);
        if (executionId != null) {
            CopyOnWriteArraySet<WebSocketSession> sessions = executionSessions.get(executionId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) executionSessions.remove(executionId);
            }
            log.debug("WebSocket disconnected for execution: {}", executionId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    }

    public void sendExecutionUpdate(String executionId, String payload) {
        CopyOnWriteArraySet<WebSocketSession> sessions = executionSessions.get(executionId);
        if (sessions != null) {
            TextMessage message = new TextMessage(payload);
            sessions.removeIf(s -> {
                if (!s.isOpen()) return true;
                try {
                    synchronized (s) {
                        s.sendMessage(message);
                    }
                } catch (IOException e) {
                    log.warn("Failed to send WS update for execution {}: {}", executionId, e.getMessage());
                }
                return false;
            });
            if (sessions.isEmpty()) executionSessions.remove(executionId);
        }
    }

    private String extractExecutionId(WebSocketSession session) {
        String path = session.getUri() != null ? session.getUri().getPath() : "";
        String prefix = "/ws/executions/";
        if (path.startsWith(prefix)) return path.substring(prefix.length());
        return null;
    }
}
