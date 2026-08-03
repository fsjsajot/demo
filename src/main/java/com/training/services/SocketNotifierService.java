package com.training.services;

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

    private final WebSocketBroadcaster broadcaster;
    private final SocketMessageCreatorService messageCreatorService;

    public SocketNotifierService(WebSocketBroadcaster broadcaster,  SocketMessageCreatorService messageCreatorService) {
        this.broadcaster = broadcaster;
        this.messageCreatorService = messageCreatorService;
    }

    @Override
    public void notifySuccess(String documentId) {
        String message = messageCreatorService.createMessage(
                "success:file-" + documentId,
                "Document " + documentId + " has been successfully processed.");

        Flux.from(broadcaster.broadcast(message))
                .subscribe(
                        ignored -> {},
                        ex -> logger.error("Failed to broadcast success message for document {}", documentId, ex)
                );
    }

    @Override
    public void notifyFailure(String documentId, String reason) {
        String message = messageCreatorService.createMessage(
                "fail:file-" + documentId,
                "Failed to process document: " + documentId + " Reason: " + reason);

        Flux.from(broadcaster.broadcast(message))
                .subscribe(
                        ignored -> {},
                        ex -> logger.error("Failed to broadcast failure message for document {}", documentId, ex)
                );
    }
}
