package com.aiflow.enterprise.notification.service;

import com.aiflow.enterprise.notification.config.KafkaConfig;
import com.aiflow.enterprise.notification.config.RabbitMQConfig;
import com.aiflow.enterprise.notification.dto.SendNotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventPublisher.class);

    private final KafkaTemplate<String, SendNotificationRequest> kafkaTemplate;
    private final RabbitTemplate rabbitTemplate;

    public NotificationEventPublisher(KafkaTemplate<String, SendNotificationRequest> kafkaTemplate,
                                      RabbitTemplate rabbitTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishToKafka(SendNotificationRequest request) {
        try {
            kafkaTemplate.send(KafkaConfig.NOTIFICATION_TOPIC, request.getCorrelationId(), request);
            log.debug("Published notification to Kafka: type={}", request.getType());
        } catch (Exception e) {
            log.error("Failed to publish to Kafka: {}", e.getMessage());
        }
    }

    public void publishToRabbitMQ(SendNotificationRequest request) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_EXCHANGE,
                    RabbitMQConfig.NOTIFICATION_ROUTING_KEY,
                    request);
            log.debug("Published notification to RabbitMQ: type={}", request.getType());
        } catch (Exception e) {
            log.error("Failed to publish to RabbitMQ: {}", e.getMessage());
        }
    }

    public void publish(SendNotificationRequest request) {
        publishToKafka(request);
        publishToRabbitMQ(request);
    }

    public void publishToDlq(SendNotificationRequest request, String reason) {
        try {
            kafkaTemplate.send(KafkaConfig.NOTIFICATION_DLQ_TOPIC, request.getCorrelationId(), request);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_DLQ_EXCHANGE,
                    RabbitMQConfig.NOTIFICATION_DLQ_ROUTING_KEY,
                    request);
            log.warn("Notification sent to DLQ: reason={}", reason);
        } catch (Exception e) {
            log.error("Failed to send to DLQ: {}", e.getMessage());
        }
    }
}
