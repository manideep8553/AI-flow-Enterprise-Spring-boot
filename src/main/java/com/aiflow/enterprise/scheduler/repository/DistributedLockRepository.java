package com.aiflow.enterprise.scheduler.repository;

import com.aiflow.enterprise.scheduler.model.DistributedLock;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface DistributedLockRepository extends MongoRepository<DistributedLock, String> {

    Optional<DistributedLock> findByLockKey(String lockKey);

    void deleteByLockKey(String lockKey);

    long deleteByExpiresAtBefore(Instant now);

    long countByExpiresAtAfter(Instant now);
}
