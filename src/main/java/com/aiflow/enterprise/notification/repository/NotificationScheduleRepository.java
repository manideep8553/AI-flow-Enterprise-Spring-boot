package com.aiflow.enterprise.notification.repository;

import com.aiflow.enterprise.notification.entity.NotificationSchedule;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationScheduleRepository extends MongoRepository<NotificationSchedule, String> {

    List<NotificationSchedule> findByProcessedFalseAndScheduledAtBefore(Instant before);

    List<NotificationSchedule> findByRecipientId(String recipientId);

    List<NotificationSchedule> findByProcessedFalse();
}
