package com.chapchap.auth.global.config.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.retry")
public record KafkaRetryProperties(
        long intervalMs,
        long maxAttempts
) {
}
