package com.aiflow.enterprise.repository;

import com.aiflow.enterprise.entity.UserSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends MongoRepository<UserSession, String> {

    List<UserSession> findByUserIdAndActiveTrue(String userId);

    Optional<UserSession> findBySessionTokenAndActiveTrue(String sessionToken);

    Optional<UserSession> findByRefreshToken(String refreshToken);

    void deleteByUserId(String userId);

    long countByUserIdAndActiveTrue(String userId);
}
