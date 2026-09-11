package com.chapchap.auth.global.messaging.kafka.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuthEvent<T>(
        String eventId,
        AuthEventType eventType,
        int version,
        OffsetDateTime occurredAt,
        Long userId,
        T data
) {
    public static <T> AuthEvent<T> create(AuthEventType eventType, Long userId, T data) {
        if (userId == null || userId <= 0) {
            throw new IllegalStateException("Kafka Event의 userId는 양수여야 합니다.");
        }
        return new AuthEvent<>(
                UUID.randomUUID().toString(),
                eventType,
                1,
                OffsetDateTime.now(),
                userId,
                data
        );
    }
}
