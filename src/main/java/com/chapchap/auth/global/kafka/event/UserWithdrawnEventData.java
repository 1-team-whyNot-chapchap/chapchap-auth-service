package com.chapchap.auth.global.kafka.event;

import java.time.OffsetDateTime;

public record UserWithdrawnEventData(OffsetDateTime withdrawnAt) {
}
