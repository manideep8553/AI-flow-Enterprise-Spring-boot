package com.aiflow.enterprise.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableScheduling
public class NotificationConfig {

    @Bean
    public Executor notificationExecutor() {
        return Executors.newFixedThreadPool(8);
    }

    @Bean
    public Executor notificationRetryExecutor() {
        return Executors.newScheduledThreadPool(4);
    }
}
