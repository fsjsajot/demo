package com.training.services;

import com.training.data.notification.NotificationEvent;
import com.training.messaging.NotificationEventPublisher;
import com.training.messaging.SocketNotifier;
import io.micronaut.websocket.WebSocketBroadcaster;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;


@Singleton
public class SocketNotifierService implements SocketNotifier {
    private static final Logger logger = LoggerFactory.getLogger(SocketNotifierService.class.getName());

    private final NotificationEventPublisher eventPublisher;

    public SocketNotifierService(NotificationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void notifySuccess(String documentId) {
        eventPublisher.publish(new NotificationEvent(
                "success:file-" + documentId,
                "Document " + documentId + " has been successfully processed."
        ));
    }

    @Override
    public void notifyFailure(String documentId, String reason) {
        eventPublisher.publish(new NotificationEvent(
                "failure:file-" + documentId,
                "Failed to process document: " + documentId + " Reason: " + reason
        ));
    }
}
