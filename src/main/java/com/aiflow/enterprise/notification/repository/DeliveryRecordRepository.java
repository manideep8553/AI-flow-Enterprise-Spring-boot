package com.aiflow.enterprise.notification.repository;

import com.aiflow.enterprise.notification.entity.DeliveryRecord;
import com.aiflow.enterprise.notification.enums.DeliveryStatus;
import com.aiflow.enterprise.notification.enums.NotificationChannel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryRecordRepository extends MongoRepository<DeliveryRecord, String> {

    List<DeliveryRecord> findByNotificationId(String notificationId);

    List<DeliveryRecord> findByNotificationIdAndChannel(String notificationId, NotificationChannel channel);

    List<DeliveryRecord> findByStatus(DeliveryStatus status);

    List<DeliveryRecord> findByDlqTrue();

    long countByStatus(DeliveryStatus status);

    long countByChannel(NotificationChannel channel);
}
