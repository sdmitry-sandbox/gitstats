package io.cx.gitstats.domain.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cx.gitstats.domain.task.ScheduledTaskPayload;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SerializationUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static String serializePayload(Object payload) {
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize task payload: " + payload, e);
        }
    }

    public static <T extends ScheduledTaskPayload> T readPayloadValue(String payload, Class<T> payloadClass) {
        try {
            return OBJECT_MAPPER.readValue(payload, payloadClass);
        } catch (Exception e) {
            var message = "Failed to deserialize task payload %s into %s class.".formatted(payload, payloadClass);
            throw new RuntimeException(message, e);
        }
    }
}
