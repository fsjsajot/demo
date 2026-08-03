package com.training.services;

import io.micronaut.websocket.WebSocketBroadcaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class SocketNotifierServiceTest {
    private WebSocketBroadcaster broadcaster;
    private SocketMessageCreatorService messageCreatorService;
    private SocketNotifierService socketNotifierService;

    @BeforeEach
    void setUp() {
        broadcaster = mock(WebSocketBroadcaster.class);
        messageCreatorService = mock(SocketMessageCreatorService.class);
        socketNotifierService = new SocketNotifierService(broadcaster, messageCreatorService);
    }

    @Test
    void shouldBroadcastSuccessMessage() {
        String documentId = "doc-1";
        String builtMessage = "{\"topic\":\"success:file-doc-1\",\"message\":\"...\"}";

        when(messageCreatorService.createMessage(eq("success:file-" + documentId), anyString()))
                .thenReturn(builtMessage);
        when(broadcaster.broadcast(builtMessage)).thenReturn(Flux.just(builtMessage));

        socketNotifierService.notifySuccess(documentId);

        verify(messageCreatorService).createMessage(eq("success:file-" + documentId), anyString());
        verify(broadcaster).broadcast(builtMessage);
    }

    @Test
    void shouldBroadcastFailureMessage() {
        String documentId = "doc-2";
        String reason = "parser error";
        String builtMessage = "{\"topic\":\"fail:file-doc-2\",\"message\":\"...\"}";

        when(messageCreatorService.createMessage(eq("fail:file-" + documentId), anyString()))
                .thenReturn(builtMessage);
        when(broadcaster.broadcast(builtMessage)).thenReturn(Flux.just(builtMessage));

        socketNotifierService.notifyFailure(documentId, reason);

        verify(messageCreatorService).createMessage(eq("fail:file-" + documentId), anyString());
        verify(broadcaster).broadcast(builtMessage);
    }

    @Test
    void shouldNotThrowWhenBroadcastErrors() {
        String documentId = "doc-3";
        String builtMessage = "{\"topic\":\"success:file-doc-3\"}";

        when(messageCreatorService.createMessage(anyString(), anyString())).thenReturn(builtMessage);
        when(broadcaster.broadcast(builtMessage))
                .thenReturn(Flux.error(new RuntimeException("simulated broadcast failure")));

        socketNotifierService.notifySuccess(documentId);

        verify(broadcaster).broadcast(builtMessage);
    }

    @Test
    void shouldLogErrorWhenBroadcastFailsForFailureNotification() {
        String documentId = "doc-4";
        String reason = "parser error";
        String builtMessage = "{\"topic\":\"fail:file-doc-4\"}";

        when(messageCreatorService.createMessage(anyString(), anyString())).thenReturn(builtMessage);
        when(broadcaster.broadcast(builtMessage))
                .thenReturn(Flux.error(new RuntimeException("simulated broadcast failure")));

        socketNotifierService.notifyFailure(documentId, reason);

        verify(broadcaster).broadcast(builtMessage);
    }
}
