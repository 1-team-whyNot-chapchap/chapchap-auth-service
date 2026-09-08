package com.chapchap.auth.global.kafka.consumer;

import com.chapchap.auth.domain.user.service.UserProjectionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class SubscriptionStatusEventConsumerTest {
    @Test
    void sendsInvalidSubscriptionStatusToRetryAndDltPath() {
        SubscriptionStatusEventConsumer consumer = new SubscriptionStatusEventConsumer(
                new SubscriptionEventParser(new ObjectMapper()), mock(UserProjectionService.class)
        );

        assertThatThrownBy(() -> consumer.consume(new ConsumerRecord<>("msa4-team1.subscription.subscription-events.v1", 0, 0L, "25", """
                {"eventId":"0198a920-6544-71cf-a923-f5728d59ad84","eventType":"SUBSCRIPTION_STATUS_CHANGED","version":1,"occurredAt":"2026-08-16T22:00:00+09:00","userId":25,"data":{"subscriptionStatus":"UNKNOWN","subscriptionVersion":1}}
                """)))
                .isInstanceOf(KafkaPayloadException.class);
    }
}
