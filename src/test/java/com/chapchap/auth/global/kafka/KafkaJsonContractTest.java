package com.chapchap.auth.global.kafka;

import com.chapchap.auth.global.kafka.consumer.SubscriptionEventParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaJsonContractTest {

    @Test
    void configuresProducerWithoutJavaTypeHeader() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yaml"));

        var properties = yaml.getObject();

        assertThat(properties)
                .containsEntry("spring.kafka.producer.key-serializer", "org.apache.kafka.common.serialization.StringSerializer")
                .containsEntry("spring.kafka.producer.value-serializer", "org.springframework.kafka.support.serializer.JsonSerializer")
                .containsEntry("spring.kafka.producer.properties.spring.json.add.type.headers", false)
                .doesNotContainKey("app.kafka.bootstrap-servers");
    }

    @Test
    void consumerDeserializesRawJsonWithoutTypeMapping() {
        String json = """
                {"eventId":"0198a920-6544-71cf-a923-f5728d59ad84","eventType":"SUBSCRIPTION_STATUS_CHANGED","version":1,"occurredAt":"2026-08-16T22:00:00+09:00","userId":25,"data":{"subscriptionStatus":"INACTIVE","subscriptionVersion":1}}
                """;
        StringDeserializer deserializer = new StringDeserializer();

        String payload = deserializer.deserialize(
                "subscription.subscription-events.v1",
                json.getBytes(StandardCharsets.UTF_8)
        );

        var event = new SubscriptionEventParser(new ObjectMapper()).parse(payload);
        assertThat(event.userId()).isEqualTo(25L);
        assertThat(event.eventType()).isEqualTo("SUBSCRIPTION_STATUS_CHANGED");
    }
}
