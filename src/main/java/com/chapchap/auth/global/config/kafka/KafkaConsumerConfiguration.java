package com.chapchap.auth.global.config.kafka;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/** Consumer는 1초 간격으로 3회 재시도 후 원본 Topic의 DLT로 보낸다. */
@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfiguration {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaRetryProperties kafkaRetryProperties;

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(record.topic() + ".DLT", record.partition())
        );
        // FixedBackOff의 maxAttempts는 최초 시도 이후 재시도 횟수다.
        // 계약대로 1초 간격 최대 3회 재시도 후 DLT로 보낸다.
        return new DefaultErrorHandler(recoverer,
                new FixedBackOff(kafkaRetryProperties.intervalMs(), kafkaRetryProperties.maxAttempts()));
    }
}
