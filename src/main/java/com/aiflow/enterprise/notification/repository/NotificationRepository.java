package com.aiflow.enterprise.notification.repository;

import com.aiflow.enterprise.notification.entity.Notification;
import com.aiflow.enterprise.notification.enums.NotificationStatus;
import com.aiflow.enterprise.notification.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    Page<Notification> findByRecipientId(String recipientId, Pageable pageable);

    Page<Notification> findByType(NotificationType type, Pageable pageable);

    Page<Notification> findByStatus(NotificationStatus status, Pageable pageable);

    Page<Notification> findByRecipientIdAndReadFalse(String recipientId, Pageable pageable);

    List<Notification> findByStatusAndNextRetryAtBefore(NotificationStatus status, Instant before);

    List<Notification> findByStatusAndExpiresAtBefore(NotificationStatus status, Instant before);

    long countByRecipientIdAndReadFalse(String recipientId);

    long countByStatus(NotificationStatus status);

    long countByType(NotificationType type);

    long countByRecipientIdAndCreatedAtAfter(String recipientId, Instant after);
}
