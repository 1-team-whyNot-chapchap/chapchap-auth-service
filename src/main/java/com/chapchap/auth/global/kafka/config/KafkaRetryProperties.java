package com.chapchap.auth.global.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.retry")
public record KafkaRetryProperties(
        long intervalMs,
        long maxAttempts
) {
}
