package com.training.messaging;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.training.services.SocketMessageCreatorService;
import io.micronaut.rabbitmq.bind.RabbitAcknowledgement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class NotificationEventConsumerTest {

    private SocketMessageCreatorService messageCreatorService;
    private NotificationEventConsumer consumer;
    private RabbitAcknowledgement acknowledgement;
    private Connection connection;
    private Channel channel;

    private static final int MAX_RETRIES = 3;
    private static final String RETRY_HEADER = "x-retry-count";

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
    void shouldRetryOnMissingSuccessfulField() {
        Map<String, Object> payload = Map.of("summary", "content", "documentId", "doc-1");

        consumer.onNotificationEvent(payload, 0, acknowledgement);

        verify(messageCreatorService, never()).createMessage(anyString(), anyString());
        verify(acknowledgement).ack();
    }

    @Test
    void shouldRetryOnNullSummary() {
        Map<String, Object> payload = Map.of( "message", "doc-1 processed.", "successful", true);

        consumer.onNotificationEvent(payload, 0, acknowledgement);

        verify(messageCreatorService, never()).createMessage(anyString(), anyString());
        verify(acknowledgement).ack();
    }

    @Test
    void shouldRetryOnNullDocumentId() {
        Map<String, Object> payload = Map.of( "summary", "Document doc-1 is processed.", "successful", true);

        consumer.onNotificationEvent(payload, 0, acknowledgement);

        verify(messageCreatorService, never()).createMessage(anyString(), anyString());
        verify(acknowledgement).ack();
    }

    @Test
    void shouldRetryOnInvalidFieldType() {
        Map<String, Object> payload = Map.of( "documentId", "file-doc-1",
                "summary", "Document doc-1 is processed.",
                "successful", "test");


        consumer.onNotificationEvent(payload, 0, acknowledgement);

        verify(messageCreatorService, never()).createMessage(anyString(), anyString());
        verify(acknowledgement).ack();
    }

    @Test
    void shouldRetryThenDeadLetterAfterMaxRetries() throws IOException, TimeoutException, InterruptedException {
        Map<String, Object> payload = Map.of( "documentId", "file-doc-1",
                "summary", "Document doc-1 is processed.",
                "successful", "test");

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            consumer.onNotificationEvent(payload, attempt, acknowledgement);
        }

        verify(acknowledgement, times(MAX_RETRIES)).ack();
        verify(acknowledgement, never()).nack(anyBoolean(), anyBoolean());

        ArgumentCaptor<AMQP.BasicProperties> amqpArgCaptor = ArgumentCaptor.forClass(AMQP.BasicProperties.class);
        verify(channel, times(MAX_RETRIES)).basicPublish(anyString(), anyString(), amqpArgCaptor.capture(), any());
        verify(channel, times(MAX_RETRIES)).close();

        verify(channel, times(MAX_RETRIES)).confirmSelect();
        verify(channel, times(MAX_RETRIES)).waitForConfirmsOrDie(anyLong());

        List<AMQP.BasicProperties> props = amqpArgCaptor.getAllValues();

        for (int i = 0; i < MAX_RETRIES; i++) {
            assertEquals(i + 1, props.get(i).getHeaders().get(RETRY_HEADER));
        }

        consumer.onNotificationEvent(payload, MAX_RETRIES, acknowledgement);
        verify(acknowledgement).nack(false, false);
    }

    @Test
    void shouldDeadLetterOriginalWhenRetryPublishFailsToConfirm() throws IOException, TimeoutException, InterruptedException {
        Map<String, Object> payload = Map.of("documentId", "file-doc-1",
                "summary", "Document doc-1 is processed.",
                "successful", "test");

        doThrow(new TimeoutException("confirm timed out"))
                .when(channel).waitForConfirmsOrDie(anyLong());

        consumer.onNotificationEvent(payload, 0, acknowledgement);

        verify(acknowledgement).nack(false, false);
        verify(acknowledgement, never()).nack(false, true);
        verify(acknowledgement, never()).ack();

        verify(channel).confirmSelect();
        verify(channel).basicPublish(anyString(), anyString(), any(), any());
        verify(channel).waitForConfirmsOrDie(anyLong());
    }

    @Test
    void shouldDeadLetterOriginalWhenBasicPublishThrows() throws IOException {
        Map<String, Object> payload = Map.of("documentId", "file-doc-1",
                "summary", "Document doc-1 is processed.",
                "successful", "test");

        doThrow(new IOException("channel closed"))
                .when(channel).basicPublish(anyString(), anyString(), any(), any());

        consumer.onNotificationEvent(payload, 0, acknowledgement);

        verify(acknowledgement).nack(false, false);
        verify(acknowledgement, never()).nack(false, true);
        verify(acknowledgement, never()).ack();
    }
}