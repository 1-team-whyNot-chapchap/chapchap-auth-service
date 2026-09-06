package com.chapchap.auth.global.kafka.consumer;

import com.chapchap.auth.global.kafka.event.SubscriptionEventEnvelope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class SubscriptionEventParser {
    private final ObjectMapper objectMapper;

    public SubscriptionEventParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SubscriptionEventEnvelope parse(String payload) {
        try {
            SubscriptionEventEnvelope event = objectMapper.readValue(payload, SubscriptionEventEnvelope.class);
            validateEnvelope(event);
            return event;
        } catch (JsonProcessingException exception) {
            throw new KafkaPayloadException("Kafka Event JSON 형식이 올바르지 않습니다.", exception);
        }
    }

    public long requiredPositiveLong(JsonNode data, String fieldName) {
        JsonNode field = data.get(fieldName);
        if (field == null || !field.canConvertToLong() || field.longValue() <= 0) {
            throw new KafkaPayloadException("Kafka Event의 " + fieldName + "은 양수 정수여야 합니다.");
        }
        return field.longValue();
    }

    public String requiredText(JsonNode data, String fieldName) {
        JsonNode field = data.get(fieldName);
        if (field == null || !field.isTextual() || field.asText().isBlank()) {
            throw new KafkaPayloadException("Kafka Event의 " + fieldName + "은 필수 문자열입니다.");
        }
        return field.asText();
    }

    private void validateEnvelope(SubscriptionEventEnvelope event) {
        if (event == null || event.eventId() == null || event.eventType() == null
                || event.version() == null || event.occurredAt() == null || event.userId() == null
                || event.data() == null || !event.data().isObject()) {
            throw new KafkaPayloadException("Kafka Event의 필수 Envelope 필드가 없습니다.");
        }
        try {
            UUID.fromString(event.eventId());
            OffsetDateTime.parse(event.occurredAt());
        } catch (IllegalArgumentException exception) {
            throw new KafkaPayloadException("Kafka Event의 eventId 또는 occurredAt 형식이 올바르지 않습니다.", exception);
        }
        if (event.version() != 1 || event.userId() <= 0) {
            throw new KafkaPayloadException("지원하지 않는 Kafka Event schema 또는 userId입니다.");
        }
    }
}
