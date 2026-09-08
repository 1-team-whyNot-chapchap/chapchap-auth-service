package com.chapchap.auth.global.kafka.producer;

import com.chapchap.auth.global.kafka.config.KafkaTopicProperties;
import com.chapchap.auth.global.kafka.event.AuthEvent;
import com.chapchap.auth.global.kafka.event.AuthEventType;
import com.chapchap.auth.global.kafka.event.UserRegisteredEventData;
import com.chapchap.auth.global.security.constant.RolePolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthEventProducerTest {
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafkaTemplate = (KafkaTemplate<String, Object>) mock(KafkaTemplate.class);
    private final AuthEventProducer producer = new AuthEventProducer(
            kafkaTemplate,
            new KafkaTopicProperties(
                    "msa4-team1.auth.user-events.v1", "msa4-team1.auth.user-events.v1.DLT",
                    "msa4-team1.subscription.address-events.v1", "msa4-team1.subscription.address-events.v1.DLT",
                    "msa4-team1.subscription.subscription-events.v1", "msa4-team1.subscription.subscription-events.v1.DLT"
            )
    );

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publishesUserRegisteredOnlyAfterTransactionCommitWithUserIdKey() {
        TransactionSynchronizationManager.initSynchronization();

        producer.publishUserRegisteredAfterCommit(25L, RolePolicy.CUSTOMER);
        verifyNoInteractions(kafkaTemplate);

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("msa4-team1.auth.user-events.v1"), eq("25"), eventCaptor.capture());
        AuthEvent<?> event = (AuthEvent<?>) eventCaptor.getValue();
        assertThat(event.eventType()).isEqualTo(AuthEventType.USER_REGISTERED);
        assertThat(event.version()).isEqualTo(1);
        assertThat(event.data()).isEqualTo(new UserRegisteredEventData("CUSTOMER"));
    }

    @Test
    void doesNotPublishWhenTransactionRollsBack() {
        TransactionSynchronizationManager.initSynchronization();

        producer.publishUserRegisteredAfterCommit(25L, RolePolicy.CUSTOMER);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verifyNoInteractions(kafkaTemplate);
    }
}
