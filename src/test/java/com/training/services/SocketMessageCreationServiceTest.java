package com.training.services;

import io.micronaut.serde.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SocketMessageCreationServiceTest {
    private ObjectMapper objectMapper;
    private SocketMessageCreatorService service;

    @BeforeEach
    public void setup() {
        objectMapper = mock(ObjectMapper.class);
        service = new SocketMessageCreatorService(objectMapper);
    }

    @Test
    void shouldReturnJsonMessageOnSuccess() throws IOException {
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"topic\":\"success\",\"message\":\"done\"}");

        String result = service.createMessage("success", "done");

        assertEquals("{\"topic\":\"success\",\"message\":\"done\"}", result);
    }

    @Test
    void shouldPassTopicAndMessageIntoMap() throws IOException {
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        service.createMessage("my-topic", "my-message");

        verify(objectMapper).writeValueAsString(argThatMapContains("topic", "my-topic", "message", "my-message"));
    }

    @Test
    void shouldReturnNullWhenSerializationFails() throws IOException {
        when(objectMapper.writeValueAsString(any())).thenThrow(new IOException("serialization failure"));

        String result = service.createMessage("topic", "message");

        assertNull(result);
    }

    private static java.util.Map<String, Object> argThatMapContains(String key1, Object value1, String key2, Object value2) {
        return org.mockito.ArgumentMatchers.argThat(map ->
                map != null
                        && value1.equals(map.get(key1))
                        && value2.equals(map.get(key2))
        );
    }
}
