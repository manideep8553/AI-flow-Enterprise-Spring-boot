package com.aiflow.enterprise.notification.channel;

import com.aiflow.enterprise.notification.enums.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ChannelFactory {

    private static final Logger log = LoggerFactory.getLogger(ChannelFactory.class);

    private final Map<NotificationChannel, NotificationSender> channels = new EnumMap<>(NotificationChannel.class);

    public ChannelFactory(List<NotificationSender> senderList) {
        for (NotificationSender sender : senderList) {
            channels.put(sender.getChannelType(), sender);
        }
        log.info("Registered {} notification senders: {}", channels.size(), channels.keySet());
    }

    public NotificationSender getSender(NotificationChannel channel) {
        NotificationSender sender = channels.get(channel);
        if (sender == null) {
            throw new IllegalArgumentException("No sender for channel: " + channel);
        }
        return sender;
    }

    public boolean isChannelAvailable(NotificationChannel channel) {
        NotificationSender sender = channels.get(channel);
        return sender != null && sender.isAvailable();
    }

    public List<NotificationChannel> getAvailableChannels() {
        return channels.values().stream()
                .filter(NotificationSender::isAvailable)
                .map(NotificationSender::getChannelType)
                .toList();
    }
}
