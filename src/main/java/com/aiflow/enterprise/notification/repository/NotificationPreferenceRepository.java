package com.aiflow.enterprise.notification.repository;

import com.aiflow.enterprise.notification.entity.NotificationPreference;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends MongoRepository<NotificationPreference, String> {

    Optional<NotificationPreference> findByUserId(String userId);

    boolean existsByUserId(String userId);
}
