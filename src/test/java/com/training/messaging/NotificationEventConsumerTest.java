package com.training.messaging;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.training.services.SocketMessageCreatorService;
import io.micronaut.rabbitmq.bind.RabbitAcknowledgement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.mockito.Mockito.*;

class NotificationEventConsumerTest {

    private SocketMessageCreatorService messageCreatorService;
    private NotificationEventConsumer consumer;
    private RabbitAcknowledgement acknowledgement;
    private Connection connection;
    private Channel channel;

    private static final int MAX_RETRIES = 3;

    @BeforeEach
    void setUp() throws IOException {
        messageCreatorService = mock(SocketMessageCreatorService.class);
        acknowledgement = mock(RabbitAcknowledgement.class);
        connection = mock(Connection.class);
        channel = mock(Channel.class);

        consumer = new NotificationEventConsumer(messageCreatorService, connection);

        when(connection.createChannel()).thenReturn(channel);
    }

    @Test
    void shouldCreateMessageFromEvent() {
        Map<String, Object> payload = Map.of("documentId", "file-doc-1",
                "summary", "Document doc-1 processed.",
                "successful", true);
        String builtJson = "{\"successful\": true, \"documentId\":\"file-doc-1\",\"summary\":\"Document doc-1 processed.\"}";

        when(messageCreatorService.createMessage("success:file-doc-1", "Document doc-1 processed."))
                .thenReturn(builtJson);

        consumer.onNotificationEvent(payload, null, acknowledgement);

        verify(messageCreatorService).createMessage("success:file-doc-1", "Document doc-1 processed.");
        verify(acknowledgement).ack();
    }

    @Test
    void shouldRequeueOnMissingSuccessfulField() {
        Map<String, Object> payload = Map.of("summary", "content", "documentId", "doc-1");

        consumer.onNotificationEvent(payload, 0, acknowledgement);

        verify(messageCreatorService, never()).createMessage(anyString(), anyString());
        verify(acknowledgement).ack();
    }

    @Test
    void shouldRequeueOnNullSummary() {
        Map<String, Object> payload = Map.of( "message", "doc-1 processed.", "successful", true);

        consumer.onNotificationEvent(payload, 0, acknowledgement);

        verify(messageCreatorService, never()).createMessage(anyString(), anyString());
        verify(acknowledgement).ack();
    }

    @Test
    void shouldRequeueOnNullDocumentId() {
        Map<String, Object> payload = Map.of( "summary", "Document doc-1 is processed.", "successful", true);

        consumer.onNotificationEvent(payload, 0, acknowledgement);

        verify(messageCreatorService, never()).createMessage(anyString(), anyString());
        verify(acknowledgement).ack();
    }

    @Test
    void shouldRequeueOnInvalidFieldType() {
        Map<String, Object> payload = Map.of( "documentId", "file-doc-1",
                "summary", "Document doc-1 is processed.",
                "successful", "test");


        consumer.onNotificationEvent(payload, 0, acknowledgement);

        verify(messageCreatorService, never()).createMessage(anyString(), anyString());
        verify(acknowledgement).ack();
    }

    @Test
    void shouldRetryThenDeadLetterAfterMaxRetries() {
        Map<String, Object> payload = Map.of( "documentId", "file-doc-1",
                "summary", "Document doc-1 is processed.",
                "successful", "test");

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            consumer.onNotificationEvent(payload, attempt, acknowledgement);
        }

        verify(acknowledgement, times(MAX_RETRIES)).ack();
        verify(acknowledgement, never()).nack(anyBoolean(), anyBoolean());

        consumer.onNotificationEvent(payload, MAX_RETRIES, acknowledgement);
        verify(acknowledgement).nack(false, false);
    }
}