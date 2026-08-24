package com.chapchap.auth.global.kafka.consumer;

import com.chapchap.auth.domain.user.service.UserProjectionService;
import com.chapchap.auth.global.kafka.event.SubscriptionEventEnvelope;
import com.chapchap.auth.global.security.constant.SubscriptionStatusPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionStatusEventConsumer {
    private final SubscriptionEventParser eventParser;
    private final UserProjectionService userProjectionService;

    @KafkaListener(topics = "${app.kafka.topics.subscription-status-events}", groupId = "${app.kafka.consumer-groups.subscription}")
    public void consume(ConsumerRecord<String, String> record) {
        SubscriptionEventEnvelope event = eventParser.parse(record.value());
        if (!String.valueOf(event.userId()).equals(record.key())) {
            throw new KafkaPayloadException("Kafka Event key와 userId가 일치하지 않습니다.");
        }
        if (!"SUBSCRIPTION_STATUS_CHANGED".equals(event.eventType())) {
            log.info("KAFKA_EVENT_SKIPPED_UNKNOWN_TYPE topic=subscription-status eventType={} userId={}",
                    event.eventType(), event.userId());
            return;
        }
        String rawStatus = eventParser.requiredText(event.data(), "subscriptionStatus");
        SubscriptionStatusPolicy status;
        try {
            status = SubscriptionStatusPolicy.valueOf(rawStatus);
        } catch (IllegalArgumentException exception) {
            throw new KafkaPayloadException("지원하지 않는 subscriptionStatus입니다.", exception);
        }
        if (status == SubscriptionStatusPolicy.UNKNOWN) {
            throw new KafkaPayloadException("UNKNOWN 구독 상태는 외부 Event로 받을 수 없습니다.");
        }
        userProjectionService.applySubscriptionStatus(
                event.userId(), status, eventParser.requiredPositiveLong(event.data(), "subscriptionVersion")
        );
    }
}
