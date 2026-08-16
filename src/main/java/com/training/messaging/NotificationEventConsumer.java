package com.training.messaging;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.training.services.SocketMessageCreatorService;
import io.micronaut.messaging.annotation.MessageHeader;
import io.micronaut.rabbitmq.annotation.Queue;
import io.micronaut.rabbitmq.annotation.RabbitListener;
import io.micronaut.rabbitmq.bind.RabbitAcknowledgement;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Singleton
@RabbitListener
public class NotificationEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventConsumer.class);

    private final SocketMessageCreatorService messageCreatorService;
    private final Connection connection;

    private static final String EXCHANGE = "parse-result.exchange";
    private static final String ROUTING_KEY = "parse-result";

    private static final String RETRY_HEADER = "x-retry-count";
    private static final int MAX_RETRIES = 3;

    public NotificationEventConsumer(SocketMessageCreatorService messageCreatorService,
                                     Connection connection) {
        this.messageCreatorService = messageCreatorService;
        this.connection = connection;
    }

    @Queue("document-parser.queue")
    public void onNotificationEvent(Map<String, Object> event,
                                    @MessageHeader(RETRY_HEADER) Integer retryCountHeader,
                                    RabbitAcknowledgement acknowledgement) {
        int retryCount = retryCountHeader == null ? 0 : retryCountHeader;
        try {
            logger.info("Received summary from parser result: {}", event.get("summary"));

            if (event.get("successful") == null) {
                throw new RuntimeException("Successful field is missing.");
            }

            if (event.get("summary") == null) {
                throw new RuntimeException("Summary is null.");
            }

            if (event.get("documentId") == null) {
                throw new RuntimeException("Document ID is null.");
            }

            if (!(event.get("successful") instanceof Boolean)) {
                throw new RuntimeException("Invalid field type: " + event.get("successful"));
            }

            String topic;

            if ((boolean) event.get("successful")) {
                topic = "success:" +  event.get("documentId");
            } else {
                topic = "failure:" +  event.get("documentId");
            }

            String json = messageCreatorService.createMessage(topic, event.get("summary").toString());
            logger.info("Created message: {}", json);

            acknowledgement.ack();
        } catch (Exception ex) {
            if (retryCount < MAX_RETRIES) {
                logger.warn("Parser result message failed (attempt {}/{}), requeueing: {}",
                        retryCount + 1, MAX_RETRIES, ex.getMessage());
                retry(event, retryCount + 1);
                acknowledgement.ack();
            } else {
                logger.error("Parser-result message exceeded {} retries, dead-lettering: {}",
                        MAX_RETRIES, ex.getMessage());
                acknowledgement.nack(false, false);
            }
        }

    }

    private void retry(Map<String, Object> event, int newRetryCount) {
        try (Channel channel = connection.createChannel()) {
            Map<String, Object> headers = new HashMap<>();
            headers.put(RETRY_HEADER, newRetryCount);

            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .headers(headers)
                    .contentType("application/json")
                    .deliveryMode(2)
                    .build();

            byte[] body = messageCreatorService.toJsonBytes(event);
            channel.basicPublish(EXCHANGE, ROUTING_KEY, props, body);
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException("Failed to republish document-parser message for retry", e);
        }
    }
}
