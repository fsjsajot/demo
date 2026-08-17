package com.training.messaging;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Singleton
public class RabbitMqInitializer implements ApplicationEventListener<StartupEvent> {

    private final Connection connection;

    public RabbitMqInitializer(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void onApplicationEvent(StartupEvent event) {
        try (Channel channel = connection.createChannel()) {
            channel.exchangeDeclare("notification-events-exchange", BuiltinExchangeType.FANOUT, true);
            channel.queueDeclare("notifications-queue", true, false, false, null);
            channel.queueBind("notifications-queue", "notification-events-exchange", "");
        } catch (IOException e) {
            throw new RuntimeException("Failed to declare RabbitMQ topology", e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}