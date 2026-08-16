package com.training.services;

import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class SocketMessageCreatorService {
    private static final Logger logger = LoggerFactory.getLogger(SocketMessageCreatorService.class);
    private final ObjectMapper objectMapper;

    public SocketMessageCreatorService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String createMessage(String topic, String message) {
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("topic", topic);
        messageMap.put("message", message);

        try {
            return objectMapper.writeValueAsString(messageMap);
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
            return null;
        }
    }

    public byte[] toJsonBytes(Map<String, Object> messageMap) {
        try {
            logger.debug("Creating JSON bytes for message: {}", messageMap);
            return objectMapper.writeValueAsBytes(messageMap);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to serialize message to JSON bytes", ex);
        }
    }
}
