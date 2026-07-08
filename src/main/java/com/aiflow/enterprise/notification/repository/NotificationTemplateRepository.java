package com.aiflow.enterprise.notification.repository;

import com.aiflow.enterprise.notification.entity.NotificationTemplate;
import com.aiflow.enterprise.notification.enums.NotificationType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends MongoRepository<NotificationTemplate, String> {

    Optional<NotificationTemplate> findByName(String name);

    List<NotificationTemplate> findByType(NotificationType type);

    Page<NotificationTemplate> findByType(NotificationType type, Pageable pageable);

    List<NotificationTemplate> findByEnabledTrue();

    List<NotificationTemplate> findByTypeAndEnabledTrue(NotificationType type);

    boolean existsByName(String name);
}
