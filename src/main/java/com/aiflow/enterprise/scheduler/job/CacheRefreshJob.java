package com.aiflow.enterprise.scheduler.job;

import com.aiflow.enterprise.scheduler.service.DistributedLockService;
import com.aiflow.enterprise.scheduler.service.JobMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

@Component
public class CacheRefreshJob extends AbstractScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(CacheRefreshJob.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    public CacheRefreshJob(DistributedLockService distributedLockService,
                            JobMonitorService jobMonitorService,
                            RedisTemplate<String, Object> redisTemplate,
                            StringRedisTemplate stringRedisTemplate) {
        super(distributedLockService, jobMonitorService);
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public String getJobName() {
        return "cache-refresh";
    }

    @Override
    public String getJobGroup() {
        return "cache";
    }

    @Override
    public String getDescription() {
        return "Refreshes Redis cache entries and evicts stale data";
    }

    @Override
    public int getLockTtlSeconds() {
        return 120;
    }

    @Override
    public int getMaxRetries() {
        return 2;
    }

    @Override
    @Scheduled(cron = "${app.scheduler.jobs.cache-refresh.cron:0 */15 * * * *}")
    public void run() {
        super.run();
    }

    @Override
    protected void execute() {
        Instant start = Instant.now();
        int refreshed = 0;
        int evicted = 0;

        Set<String> cacheKeys = stringRedisTemplate.keys("cache:*");
        if (cacheKeys == null || cacheKeys.isEmpty()) {
            log.info("No cache entries to refresh");
            return;
        }

        for (String key : cacheKeys) {
            try {
                String ttlStr = (String) redisTemplate.opsForHash().get(key, "ttl");
                String staleAtStr = (String) redisTemplate.opsForHash().get(key, "staleAt");

                if (staleAtStr != null) {
                    Instant staleAt = Instant.parse(staleAtStr);
                    if (staleAt.isBefore(Instant.now())) {
                        stringRedisTemplate.delete(key);
                        evicted++;
                    }
                }

                if (ttlStr != null) {
                    long ttl = Long.parseLong(ttlStr);
                    Boolean expired = stringRedisTemplate.getExpire(key) != null
                            && stringRedisTemplate.getExpire(key) < ttl / 4;
                    if (Boolean.TRUE.equals(expired)) {
                        redisTemplate.opsForHash().increment(key, "refreshCount", 1);
                        stringRedisTemplate.expire(key, java.time.Duration.ofSeconds(ttl));
                        refreshed++;
                    }
                }

            } catch (Exception e) {
                log.warn("Failed to refresh cache key {}: {}", key, e.getMessage());
            }
        }

        long elapsed = java.time.Duration.between(start, Instant.now()).toMillis();
        log.info("Cache refresh complete: {} refreshed, {} evicted in {}ms", refreshed, evicted, elapsed);
    }
}
