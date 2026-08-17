package com.training.messaging;

import com.training.data.notification.NotificationEvent;
import io.micronaut.rabbitmq.annotation.Binding;
import io.micronaut.rabbitmq.annotation.RabbitClient;

@RabbitClient("notification-events-exchange")
public interface NotificationEventPublisher {

    @Binding("notification-event-routing-key")
    void publish(NotificationEvent notificationEvent);
}
