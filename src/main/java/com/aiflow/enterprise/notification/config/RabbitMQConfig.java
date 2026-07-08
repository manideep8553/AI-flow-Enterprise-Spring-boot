package com.aiflow.enterprise.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    public static final String NOTIFICATION_QUEUE = "notifications.queue";
    public static final String NOTIFICATION_DLQ = "notifications.dlq";
    public static final String NOTIFICATION_RETRY_QUEUE = "notifications.retry";
    public static final String NOTIFICATION_EXCHANGE = "notifications.exchange";
    public static final String NOTIFICATION_DLQ_EXCHANGE = "notifications.dlq.exchange";
    public static final String NOTIFICATION_ROUTING_KEY = "notification.send";
    public static final String NOTIFICATION_DLQ_ROUTING_KEY = "notification.dlq";

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_DLQ_ROUTING_KEY)
                .withArgument("x-max-retries", 3)
                .build();
    }

    @Bean
    public Queue notificationDlq() {
        return QueueBuilder.durable(NOTIFICATION_DLQ).build();
    }

    @Bean
    public Queue notificationRetryQueue() {
        return QueueBuilder.durable(NOTIFICATION_RETRY_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_ROUTING_KEY)
                .withArgument("x-message-ttl", 30000)
                .build();
    }

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public DirectExchange notificationDlqExchange() {
        return new DirectExchange(NOTIFICATION_DLQ_EXCHANGE);
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(notificationExchange())
                .with(NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public Binding notificationDlqBinding() {
        return BindingBuilder.bind(notificationDlq())
                .to(notificationDlqExchange())
                .with(NOTIFICATION_DLQ_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
