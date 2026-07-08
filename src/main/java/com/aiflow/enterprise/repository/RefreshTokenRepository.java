package com.aiflow.enterprise.repository;

import com.aiflow.enterprise.entity.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUserId(String userId);

    List<RefreshToken> findByUserIdAndRevokedFalse(String userId);

    List<RefreshToken> findByExpiresAtBefore(Instant date);

    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    void deleteByUserId(String userId);

    void deleteByExpiresAtBefore(Instant date);
}
