package com.chapchap.auth.domain.user.service;

import com.chapchap.auth.domain.user.entity.User;
import com.chapchap.auth.domain.user.repository.UserRepository;
import com.chapchap.auth.global.security.constant.SubscriptionStatusPolicy;
import com.chapchap.auth.global.security.constant.UserStatusPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 다른 서비스 Event를 Auth DB의 최소 Projection에 반영한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProjectionService {
    private final UserRepository userRepository;

    @Transactional
    public void applyDefaultAddress(Long userId, Long defaultAddressId, long addressVersion) {
        userRepository.findByIdForUpdate(userId).ifPresentOrElse(user -> {
            if (user.getStatus() == UserStatusPolicy.WITHDRAWN) {
                log.info("KAFKA_PROJECTION_SKIPPED_WITHDRAWN projection=defaultAddress userId={}", userId);
                return;
            }
            if (!user.updateDefaultAddressProjection(defaultAddressId, addressVersion)) {
                log.info("KAFKA_PROJECTION_SKIPPED_STALE projection=defaultAddress userId={} version={}", userId, addressVersion);
            }
        }, () -> log.info("KAFKA_PROJECTION_SKIPPED_USER_NOT_FOUND projection=defaultAddress userId={}", userId));
    }

    @Transactional
    public void applySubscriptionStatus(Long userId, SubscriptionStatusPolicy subscriptionStatus, long subscriptionVersion) {
        userRepository.findByIdForUpdate(userId).ifPresentOrElse(user -> {
            if (user.getStatus() == UserStatusPolicy.WITHDRAWN) {
                log.info("KAFKA_PROJECTION_SKIPPED_WITHDRAWN projection=subscription userId={}", userId);
                return;
            }
            if (!user.updateSubscriptionProjection(subscriptionStatus, subscriptionVersion)) {
                log.info("KAFKA_PROJECTION_SKIPPED_STALE projection=subscription userId={} version={}", userId, subscriptionVersion);
            }
        }, () -> log.info("KAFKA_PROJECTION_SKIPPED_USER_NOT_FOUND projection=subscription userId={}", userId));
    }
}
