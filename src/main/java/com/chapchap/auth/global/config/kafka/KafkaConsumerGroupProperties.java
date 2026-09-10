package com.chapchap.auth.global.config.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.consumer-groups")
public record KafkaConsumerGroupProperties(
        String address,
        String subscription
) {
}
