package com.chapchap.auth.global.config.json;

import com.chapchap.auth.global.messaging.kafka.consumer.SubscriptionEventParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyJsonConfigurationTest {
    @Test
    void wiresExistingKafkaParserAndPreservesJsonResponses() {
        new ApplicationContextRunner()
                .withUserConfiguration(LegacyJsonConfiguration.class, SubscriptionEventParser.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ObjectMapper.class);
                    assertThat(context).hasSingleBean(SubscriptionEventParser.class);
                    ObjectMapper mapper = context.getBean(ObjectMapper.class);
                    String response = mapper.writeValueAsString(Map.of("message", "인증이 필요합니다."));
                    assertThat(mapper.readTree(response).get("message").asText())
                            .isEqualTo("인증이 필요합니다.");
                });
    }
}
