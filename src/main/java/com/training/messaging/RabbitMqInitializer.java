package com.training.messaging;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Singleton
public class RabbitMqInitializer implements ApplicationEventListener<StartupEvent> {

    private static final String EXCHANGE = "parse-result.exchange";
    private static final String QUEUE = "parse-result.queue";
    private static final String ROUTING_KEY = "parse-result";

    private static final String DLX = "parse-result.dlx";
    private static final String DEAD_QUEUE = "parse-result.dead.queue";

    private final Connection connection;

    public RabbitMqInitializer(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void onApplicationEvent(StartupEvent event) {
        try (Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(EXCHANGE, BuiltinExchangeType.DIRECT, true);
            channel.queueDeclare(QUEUE, true, false, false, Map.of(
                    "x-dead-letter-exchange", DLX,
                    "x-dead-letter-routing-key", ROUTING_KEY
            ));
            channel.queueBind(QUEUE, EXCHANGE, ROUTING_KEY);

            channel.exchangeDeclare(DLX, BuiltinExchangeType.DIRECT, true);
            channel.queueDeclare(DEAD_QUEUE, true, false, false, null);
            channel.queueBind(DEAD_QUEUE, DLX, ROUTING_KEY);
        } catch (IOException e) {
            throw new RuntimeException("Failed to declare RabbitMQ topology", e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}