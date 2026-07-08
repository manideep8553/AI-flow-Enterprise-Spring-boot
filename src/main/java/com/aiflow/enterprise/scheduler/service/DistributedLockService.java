package com.aiflow.enterprise.scheduler.service;

import com.aiflow.enterprise.scheduler.model.DistributedLock;
import com.aiflow.enterprise.scheduler.repository.DistributedLockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class DistributedLockService {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockService.class);

    private final DistributedLockRepository lockRepository;
    private final MongoTemplate mongoTemplate;

    public DistributedLockService(DistributedLockRepository lockRepository,
                                   MongoTemplate mongoTemplate) {
        this.lockRepository = lockRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public boolean acquireLock(String lockKey, String acquiredBy, int ttlSeconds) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(ttlSeconds);

        DistributedLock lock = DistributedLock.builder()
                .lockKey(lockKey)
                .acquiredBy(acquiredBy)
                .acquiredAt(now)
                .expiresAt(expiresAt)
                .ttlSeconds(ttlSeconds)
                .version(1)
                .build();

        try {
            Query query = Query.query(Criteria.where("lockKey").is(lockKey));
            Update update = new Update()
                    .setOnInsert("lockKey", lockKey)
                    .setOnInsert("acquiredBy", acquiredBy)
                    .setOnInsert("acquiredAt", now)
                    .setOnInsert("expiresAt", expiresAt)
                    .setOnInsert("ttlSeconds", ttlSeconds)
                    .setOnInsert("version", 1);

            DistributedLock existing = mongoTemplate.findAndModify(
                    query,
                    update,
                    FindAndModifyOptions.options().upsert(true).returnNew(true),
                    DistributedLock.class
            );

            if (existing == null) {
                return true;
            }

            boolean isExpired = existing.getExpiresAt() != null && existing.getExpiresAt().isBefore(now);
            if (isExpired) {
                Update updateExisting = new Update()
                        .set("acquiredBy", acquiredBy)
                        .set("acquiredAt", now)
                        .set("expiresAt", expiresAt)
                        .set("ttlSeconds", ttlSeconds)
                        .inc("version", 1);

                var refreshed = mongoTemplate.findAndModify(
                        Query.query(Criteria.where("lockKey").is(lockKey)
                                .and("expiresAt").is(existing.getExpiresAt())),
                        updateExisting,
                        FindAndModifyOptions.options().returnNew(true),
                        DistributedLock.class
                );

                boolean acquired = refreshed != null && acquiredBy.equals(refreshed.getAcquiredBy());
                if (acquired) {
                    log.info("Lock acquired (stale): {} by {}", lockKey, acquiredBy);
                }
                return acquired;
            }

            boolean owned = acquiredBy.equals(existing.getAcquiredBy());
            if (owned) {
                mongoTemplate.findAndModify(
                        Query.query(Criteria.where("lockKey").is(lockKey)
                                .and("acquiredBy").is(acquiredBy)),
                        new Update().set("expiresAt", expiresAt),
                        DistributedLock.class
                );
                return true;
            }

            return false;

        } catch (Exception e) {
            log.warn("Failed to acquire lock {}: {}", lockKey, e.getMessage());
            return false;
        }
    }

    public boolean acquireLock(String lockKey, int ttlSeconds) {
        String nodeId = getNodeId();
        return acquireLock(lockKey, nodeId, ttlSeconds);
    }

    public boolean isLocked(String lockKey) {
        return lockRepository.findByLockKey(lockKey)
                .map(lock -> lock.getExpiresAt() != null && lock.getExpiresAt().isAfter(Instant.now()))
                .orElse(false);
    }

    public void releaseLock(String lockKey) {
        lockRepository.deleteByLockKey(lockKey);
        log.debug("Lock released: {}", lockKey);
    }

    public void releaseLock(String lockKey, String acquiredBy) {
        mongoTemplate.findAndRemove(
                Query.query(Criteria.where("lockKey").is(lockKey)
                        .and("acquiredBy").is(acquiredBy)),
                DistributedLock.class
        );
        log.debug("Lock released: {} by {}", lockKey, acquiredBy);
    }

    public void cleanupExpiredLocks() {
        long removed = lockRepository.deleteByExpiresAtBefore(Instant.now());
        if (removed > 0) {
            log.info("Cleaned up {} expired locks", removed);
        }
    }

    public long getActiveLockCount() {
        return lockRepository.countByExpiresAtAfter(Instant.now());
    }

    private String getNodeId() {
        String host = System.getenv("HOSTNAME");
        if (host == null || host.isBlank()) {
            try {
                host = java.net.InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                host = "unknown-" + Instant.now().toEpochMilli();
            }
        }
        return host;
    }
}
