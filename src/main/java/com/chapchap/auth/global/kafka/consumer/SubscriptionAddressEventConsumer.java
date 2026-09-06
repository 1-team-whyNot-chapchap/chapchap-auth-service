package com.chapchap.auth.global.kafka.consumer;

import com.chapchap.auth.domain.user.service.UserProjectionService;
import com.chapchap.auth.global.kafka.event.SubscriptionEventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionAddressEventConsumer {
    private final SubscriptionEventParser eventParser;
    private final UserProjectionService userProjectionService;

    @KafkaListener(topics = "${app.kafka.topics.subscription-address-events}", groupId = "${app.kafka.consumer-groups.address}")
    public void consume(ConsumerRecord<String, String> record) {
        SubscriptionEventEnvelope event = eventParser.parse(record.value());
        assertUserKey(record.key(), event.userId());
        switch (event.eventType()) {
            case "DEFAULT_ADDRESS_CHANGED" -> userProjectionService.applyDefaultAddress(
                    event.userId(),
                    eventParser.requiredPositiveLong(event.data(), "defaultAddressId"),
                    eventParser.requiredPositiveLong(event.data(), "addressVersion")
            );
            case "DEFAULT_ADDRESS_CLEARED" -> userProjectionService.applyDefaultAddress(
                    event.userId(),
                    null,
                    eventParser.requiredPositiveLong(event.data(), "addressVersion")
            );
            default -> log.info("KAFKA_EVENT_SKIPPED_UNKNOWN_TYPE topic=subscription-address eventType={} userId={}",
                    event.eventType(), event.userId());
        }
    }

    private void assertUserKey(String key, Long userId) {
        if (!String.valueOf(userId).equals(key)) {
            throw new KafkaPayloadException("Kafka Event key와 userId가 일치하지 않습니다.");
        }
    }
}
