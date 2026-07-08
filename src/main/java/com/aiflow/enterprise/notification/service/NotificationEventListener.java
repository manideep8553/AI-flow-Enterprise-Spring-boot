package com.aiflow.enterprise.notification.service;

import com.aiflow.enterprise.notification.dto.SendNotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationOrchestrator orchestrator;

    public NotificationEventListener(NotificationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @KafkaListener(topics = "${notification.kafka.topic:notifications}", groupId = "notification-group")
    public void onKafkaNotification(SendNotificationRequest request) {
        log.debug("Received notification from Kafka: type={} correlationId={}",
                request.getType(), request.getCorrelationId());
        orchestrator.processNotificationRequest(request);
    }

    @KafkaListener(topics = "${notification.kafka.retry-topic:notifications-retry}", groupId = "notification-retry-group")
    public void onKafkaRetry(SendNotificationRequest request) {
        log.debug("Received retry notification from Kafka: correlationId={}", request.getCorrelationId());
        orchestrator.processNotificationRequest(request);
    }

    @RabbitListener(queues = "${notification.rabbitmq.queue:notifications.queue}")
    public void onRabbitNotification(SendNotificationRequest request) {
        log.debug("Received notification from RabbitMQ: type={} correlationId={}",
                request.getType(), request.getCorrelationId());
        orchestrator.processNotificationRequest(request);
    }
}
