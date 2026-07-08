package com.aiflow.enterprise.notification.service;

import com.aiflow.enterprise.notification.entity.NotificationSchedule;
import com.aiflow.enterprise.notification.repository.NotificationScheduleRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executor;

@Service
public class NotificationSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(NotificationSchedulerService.class);

    private final NotificationScheduleRepository scheduleRepository;
    private final NotificationOrchestrator orchestrator;
    private final Executor notificationExecutor;

    public NotificationSchedulerService(NotificationScheduleRepository scheduleRepository,
                                        NotificationOrchestrator orchestrator,
                                        Executor notificationExecutor) {
        this.scheduleRepository = scheduleRepository;
        this.orchestrator = orchestrator;
        this.notificationExecutor = notificationExecutor;
    }

    @Scheduled(fixedRate = 30000)
    public void processScheduledNotifications() {
        List<NotificationSchedule> due = scheduleRepository
                .findByProcessedFalseAndScheduledAtBefore(Instant.now());

        for (NotificationSchedule schedule : due) {
            notificationExecutor.execute(() -> {
                try {
                    schedule.setProcessed(true);
                    schedule.setProcessedAt(Instant.now());
                    scheduleRepository.save(schedule);
                    log.info("Processed scheduled notification: id={}", schedule.getId());
                } catch (Exception e) {
                    log.error("Failed to process scheduled notification {}: {}",
                            schedule.getId(), e.getMessage());
                }
            });
        }
    }

    @Scheduled(fixedRate = 60000)
    public void retryFailedNotifications() {
        log.debug("Checking for failed notifications to retry...");
    }
}
