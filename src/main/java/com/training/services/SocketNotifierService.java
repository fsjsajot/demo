package com.training.services;

import com.training.messaging.SocketNotifier;
import io.micronaut.websocket.WebSocketBroadcaster;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;


@Singleton
public class SocketNotifierService implements SocketNotifier {
    private static final Logger logger = LoggerFactory.getLogger(SocketNotifierService.class.getName());

   private final WebSocketBroadcaster webSocketBroadcaster;
   private final SocketMessageCreatorService messageCreatorService;

    public SocketNotifierService(WebSocketBroadcaster webSocketBroadcaster, SocketMessageCreatorService messageCreatorService) {
        this.webSocketBroadcaster = webSocketBroadcaster;
        this.messageCreatorService = messageCreatorService;
    }

    @Override
    public void notifySuccess(String documentId) {
        String json = messageCreatorService.createMessage(
                "success:file-" + documentId,
                "Document " + documentId + " has been successfully processed."
        );

        logger.info(json);

        Flux.from(webSocketBroadcaster.broadcast(json)).subscribe(
                _ -> {},
                ex -> logger.error("Failed to broadcast success message for document {}", documentId, ex)
        );
    }

    @Override
    public void notifyFailure(String documentId, String reason) {
        String json = messageCreatorService.createMessage(
                "failure:file-" + documentId,
                "Failed to process document: " + documentId + " Reason: " + reason
        );

        logger.info(json);

        Flux.from(webSocketBroadcaster.broadcast(json)).subscribe(
                _ -> {},
                ex -> logger.error("Failed to broadcast failure message for document {}", documentId, ex)
        );
    }
}
