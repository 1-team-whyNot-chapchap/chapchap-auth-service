package com.chapchap.auth.global.kafka.producer;

import com.chapchap.auth.global.kafka.config.KafkaTopicProperties;
import com.chapchap.auth.global.kafka.event.AuthEvent;
import com.chapchap.auth.global.kafka.event.AuthEventType;
import com.chapchap.auth.global.kafka.event.AdminAccountDisabledEventData;
import com.chapchap.auth.global.kafka.event.UserRegisteredEventData;
import com.chapchap.auth.global.kafka.event.UserWithdrawnEventData;
import com.chapchap.auth.global.kafka.event.UserRoleChangedEventData;
import com.chapchap.auth.global.security.constant.RolePolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicProperties kafkaTopicProperties;

    public void publishUserRegisteredAfterCommit(Long userId, RolePolicy role) {
        publishAfterCommit(AuthEventType.USER_REGISTERED, () -> AuthEvent.create(
                AuthEventType.USER_REGISTERED,
                userId,
                new UserRegisteredEventData(role.name())
        ));
    }

    public void publishUserWithdrawnAfterCommit(Long userId, LocalDateTime withdrawnAt) {
        publishAfterCommit(AuthEventType.USER_WITHDRAWN, () -> AuthEvent.create(
                AuthEventType.USER_WITHDRAWN,
                userId,
                new UserWithdrawnEventData(withdrawnAt.atZone(ZoneId.systemDefault()).toOffsetDateTime())
        ));
    }

    public void publishAdminAccountDisabledAfterCommit(Long userId, LocalDateTime disabledAt) {
        publishAfterCommit(AuthEventType.ADMIN_ACCOUNT_DISABLED, () -> AuthEvent.create(
                AuthEventType.ADMIN_ACCOUNT_DISABLED,
                userId,
                new AdminAccountDisabledEventData(disabledAt.atZone(ZoneId.systemDefault()).toOffsetDateTime())
        ));
    }

    public void publishUserRoleChangedAfterCommit(Long userId, RolePolicy previousRole, RolePolicy newRole) {
        publishAfterCommit(AuthEventType.USER_ROLE_CHANGED, () -> AuthEvent.create(
                AuthEventType.USER_ROLE_CHANGED,
                userId,
                new UserRoleChangedEventData(previousRole.name(), newRole.name())
        ));
    }

    private void publishAfterCommit(AuthEventType eventType, Supplier<AuthEvent<?>> eventSupplier) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            log.error("KAFKA_PUBLISH_SKIPPED_NO_TRANSACTION eventType={}", eventType);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                AuthEvent<?> event = eventSupplier.get();
                kafkaTemplate.send(kafkaTopicProperties.authUserEvents(), String.valueOf(event.userId()), event)
                        .whenComplete((result, exception) -> {
                            if (exception != null) {
                                log.error(
                                        "KAFKA_PUBLISH_FAILED eventId={} eventType={} userId={} occurredAt={}",
                                        event.eventId(), event.eventType(), event.userId(), event.occurredAt()
                                );
                            }
                        });
            }
        });
    }
}
