package com.training.messaging;

import com.training.data.notification.NotificationEvent;
import com.training.services.SocketMessageCreatorService;
import io.micronaut.websocket.WebSocketBroadcaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class NotificationEventConsumerTest {

    private WebSocketBroadcaster broadcaster;
    private SocketMessageCreatorService messageCreatorService;
    private NotificationEventConsumer consumer;

    @BeforeEach
    void setUp() {
        broadcaster = mock(WebSocketBroadcaster.class);
        messageCreatorService = mock(SocketMessageCreatorService.class);
        consumer = new NotificationEventConsumer(broadcaster, messageCreatorService);
    }

    @Test
    void shouldBroadcastMessageBuiltFromEvent() {
        NotificationEvent event = new NotificationEvent("success:file-doc-1", "Document doc-1 processed.");
        String builtJson = "{\"topic\":\"success:file-doc-1\",\"message\":\"...\"}";

        when(messageCreatorService.createMessage("success:file-doc-1", "Document doc-1 processed."))
                .thenReturn(builtJson);
        when(broadcaster.broadcast(builtJson)).thenReturn(Flux.just(builtJson));

        consumer.onNotificationEvent(event);

        verify(messageCreatorService).createMessage("success:file-doc-1", "Document doc-1 processed.");
        verify(broadcaster).broadcast(builtJson);
    }

    @Test
    void shouldNotThrowWhenBroadcastFails() {
        NotificationEvent event = new NotificationEvent("failure:file-doc-2", "Failed to process.");
        String builtJson = "{\"topic\":\"failure:file-doc-2\"}";

        when(messageCreatorService.createMessage(anyString(), anyString())).thenReturn(builtJson);
        when(broadcaster.broadcast(builtJson))
                .thenReturn(Flux.error(new RuntimeException("simulated broadcast failure")));

        consumer.onNotificationEvent(event);

        verify(broadcaster).broadcast(builtJson);
    }
}