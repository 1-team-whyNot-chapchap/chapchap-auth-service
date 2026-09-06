package com.chapchap.auth.global.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topics")
public record KafkaTopicProperties(
        String authUserEvents,
        String authUserEventsDlt,
        String subscriptionAddressEvents,
        String subscriptionAddressEventsDlt,
        String subscriptionStatusEvents,
        String subscriptionStatusEventsDlt
) {
}
