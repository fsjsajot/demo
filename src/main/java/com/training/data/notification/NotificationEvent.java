package com.training.data.notification;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class NotificationEvent {
    private String topic;
    private String message;

    public NotificationEvent(String topic, String message) {
        this.topic = topic;
        this.message = message;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
