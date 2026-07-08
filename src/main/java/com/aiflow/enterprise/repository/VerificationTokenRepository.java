package com.aiflow.enterprise.repository;

import com.aiflow.enterprise.entity.VerificationToken;
import com.aiflow.enterprise.enums.TokenType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends MongoRepository<VerificationToken, String> {

    Optional<VerificationToken> findByToken(String token);

    Optional<VerificationToken> findByUserIdAndTokenTypeAndUsedFalse(String userId, TokenType tokenType);

    Optional<VerificationToken> findByEmailAndTokenTypeAndUsedFalse(String email, TokenType tokenType);

    void deleteByExpiresAtBefore(Instant date);
}
