package com.training.messaging;

import com.training.data.notification.NotificationEvent;
import com.training.services.SocketMessageCreatorService;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import io.micronaut.websocket.WebSocketBroadcaster;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

@Singleton
@RabbitListener
public class NotificationEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventConsumer.class);

    private final WebSocketBroadcaster broadcaster;
    private final SocketMessageCreatorService messageCreatorService;

    public NotificationEventConsumer(WebSocketBroadcaster broadcaster,
                                     SocketMessageCreatorService messageCreatorService) {
        this.broadcaster = broadcaster;
        this.messageCreatorService = messageCreatorService;
    }

    @Queue("notifications-queue")
    public void onNotificationEvent(NotificationEvent event) {
        String json = messageCreatorService.createMessage(event.getTopic(), event.getMessage());

        Flux.from(broadcaster.broadcast(json))
                .subscribe(
                        ignored -> {},
                        ex -> logger.error("Failed to broadcast notification for topic {}", event.getTopic(), ex)
                );
    }
}
