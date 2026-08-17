package com.training.websocket;

import com.training.services.SocketMessageCreatorService;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.websocket.WebSocketBroadcaster;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.micronaut.websocket.annotation.ServerWebSocket;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@ServerWebSocket("/ws/notifier/{topic}")
public class WebSocketNotifier {

    private static final Logger LOG = LoggerFactory.getLogger(WebSocketNotifier.class);
    private final WebSocketBroadcaster broadcaster;
    private final SocketMessageCreatorService messageCreatorService;

    public WebSocketNotifier(WebSocketBroadcaster broadcaster,  SocketMessageCreatorService messageCreatorService) {
        this.broadcaster = broadcaster;
        this.messageCreatorService = messageCreatorService;
    }

    @OnOpen
    public Publisher<String> onOpen(String topic, WebSocketSession session) {
        log("onOpen", session, topic);

        return broadcaster.broadcast(messageCreatorService.createMessage(topic, "Joined the websocket topic: " + topic));
    }

    @OnMessage
    public Publisher<String> onMessage(String topic, WebSocketSession session, String message) {
        log("onMessage", session, topic);
        return broadcaster.broadcast(message);
    }

    @OnClose
    public Publisher<String> onClose(String topic, WebSocketSession session) {
        log("onClose", session, topic);

        return broadcaster.broadcast(messageCreatorService.createMessage(topic, "Closed the websocket topic: " + topic));
    }

    private void log(String event, WebSocketSession session, String topic) {
        LOG.info("* WebSocket: {} received for session {} regarding '{}'",
                event, session.getId(), topic);
    }
}
