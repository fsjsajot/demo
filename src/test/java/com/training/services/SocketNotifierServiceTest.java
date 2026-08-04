package com.training.services;

import com.training.messaging.NotificationEventPublisher;
import io.micronaut.websocket.WebSocketBroadcaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class SocketNotifierServiceTest {
    private NotificationEventPublisher eventPublisher;
    private SocketNotifierService socketNotifierService;

    @BeforeEach
    void setUp() {
        eventPublisher = mock(NotificationEventPublisher.class);
        socketNotifierService = new SocketNotifierService(eventPublisher);
    }

    @Test
    void shouldPublishSuccessEvent() {
        String documentId = "doc-1";

        socketNotifierService.notifySuccess(documentId);

        verify(eventPublisher).publish(argThat(event ->
                ("success:file-" + documentId).equals(event.getTopic())  &&
                event.getMessage().contains(documentId)
        ));
    }

    @Test
    void shouldBroadcastFailureMessage() {
        String documentId = "doc-2";
        String reason = "parser error";

        socketNotifierService.notifyFailure(documentId, reason);

        verify(eventPublisher).publish(argThat(event ->
                ("failure:file-" + documentId).equals(event.getTopic()) &&
                event.getMessage().contains(documentId) &&
                event.getMessage().contains(reason)
        ));
    }

//    @Test
//    void shouldNotThrowWhenBroadcastErrors() {
//        String documentId = "doc-3";
//        String builtMessage = "{\"topic\":\"success:file-doc-3\"}";
//
//        when(messageCreatorService.createMessage(anyString(), anyString())).thenReturn(builtMessage);
//        when(broadcaster.broadcast(builtMessage))
//                .thenReturn(Flux.error(new RuntimeException("simulated broadcast failure")));
//
//        socketNotifierService.notifySuccess(documentId);
//
//        verify(broadcaster).broadcast(builtMessage);
//    }
//
//    @Test
//    void shouldLogErrorWhenBroadcastFailsForFailureNotification() {
//        String documentId = "doc-4";
//        String reason = "parser error";
//        String builtMessage = "{\"topic\":\"fail:file-doc-4\"}";
//
//        when(messageCreatorService.createMessage(anyString(), anyString())).thenReturn(builtMessage);
//        when(broadcaster.broadcast(builtMessage))
//                .thenReturn(Flux.error(new RuntimeException("simulated broadcast failure")));
//
//        socketNotifierService.notifyFailure(documentId, reason);
//
//        verify(broadcaster).broadcast(builtMessage);
//    }
}
