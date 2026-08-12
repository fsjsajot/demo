package com.training.services;

import com.training.messaging.NotificationEventPublisher;
import com.training.messaging.SocketNotifier;
import io.micronaut.websocket.WebSocketBroadcaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class SocketNotifierServiceTest {
    private SocketNotifier socketNotifier;
    private WebSocketBroadcaster webSocketBroadcaster;
    private SocketMessageCreatorService messageCreatorService;

    @BeforeEach
    void setUp() {
        webSocketBroadcaster = mock(WebSocketBroadcaster.class);
        messageCreatorService = mock(SocketMessageCreatorService.class);

        socketNotifier = new SocketNotifierService(webSocketBroadcaster, messageCreatorService);
    }

    @Test
    void shouldBroadcastSuccessMessage() {
        String documentId = "doc-1";
        String builtMessage = "{\"topic\":\"success:file-doc-1\"}";

        when(messageCreatorService.createMessage(eq("success:file-" + documentId), anyString()))
                .thenReturn(builtMessage);
        when(webSocketBroadcaster.broadcast(builtMessage)).thenReturn(Flux.just(builtMessage));

        socketNotifier.notifySuccess(documentId);

        verify(messageCreatorService).createMessage(
                eq("success:file-" + documentId),
                contains(documentId)
        );
        verify(webSocketBroadcaster).broadcast(builtMessage);
    }

    @Test
    void shouldBroadcastFailureMessage() {
        String documentId = "doc-2";
        String reason = "parser error";
        String builtMessage = "{\"topic\":\"failure:file-doc-2\"}";

        when(messageCreatorService.createMessage(eq("failure:file-" + documentId), anyString()))
                .thenReturn(builtMessage);
        when(webSocketBroadcaster.broadcast(builtMessage)).thenReturn(Flux.just(builtMessage));

        socketNotifier.notifyFailure(documentId, reason);

        verify(messageCreatorService).createMessage(
                eq("failure:file-" + documentId),
                argThat(msg -> msg.contains(documentId) && msg.contains(reason))
        );
        verify(webSocketBroadcaster).broadcast(builtMessage);
    }

    @Test
    void shouldNotThrowWhenBroadcastErrors() {
        String documentId = "doc-3";
        String builtMessage = "{\"topic\":\"success:file-doc-3\"}";

        when(messageCreatorService.createMessage(anyString(), anyString())).thenReturn(builtMessage);
        when(webSocketBroadcaster.broadcast(builtMessage))
                .thenReturn(Flux.error(new RuntimeException("simulated broadcast failure")));

        assertDoesNotThrow(() -> socketNotifier.notifySuccess(documentId));
        verify(webSocketBroadcaster).broadcast(builtMessage);
    }

    @Test
    void shouldNotThrowWhenBroadcastErrorsOnFailure() {
        String documentId = "doc-4";
        String reason = "parser error";
        String builtMessage = "{\"topic\":\"failure:file-doc-4\"}";

        when(messageCreatorService.createMessage(anyString(), anyString())).thenReturn(builtMessage);
        when(webSocketBroadcaster.broadcast(builtMessage))
                .thenReturn(Flux.error(new RuntimeException("simulated broadcast failure")));

        assertDoesNotThrow(() -> socketNotifier.notifyFailure(documentId, reason));

        verify(webSocketBroadcaster).broadcast(builtMessage);
    }
}
