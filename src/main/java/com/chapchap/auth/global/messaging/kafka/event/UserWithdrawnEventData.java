package com.chapchap.auth.global.messaging.kafka.event;

import java.time.OffsetDateTime;

public record UserWithdrawnEventData(OffsetDateTime withdrawnAt) {
}
