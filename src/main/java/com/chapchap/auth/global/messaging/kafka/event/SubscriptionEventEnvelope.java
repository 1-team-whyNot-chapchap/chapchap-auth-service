package com.chapchap.auth.global.messaging.kafka.event;

import com.fasterxml.jackson.databind.JsonNode;

/** 외부 서비스 Event는 역호환 가능한 Tree 형태로 받아 필요한 필드만 검증한다. */
public record SubscriptionEventEnvelope(
        String eventId,
        String eventType,
        Integer version,
        String occurredAt,
        Long userId,
        JsonNode data
) {
}
