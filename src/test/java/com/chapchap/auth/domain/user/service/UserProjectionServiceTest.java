package com.chapchap.auth.domain.user.service;

import com.chapchap.auth.domain.user.entity.User;
import com.chapchap.auth.domain.user.repository.UserRepository;
import com.chapchap.auth.domain.user.constant.SubscriptionStatusPolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UserProjectionServiceTest {
    @Test
    void appliesOnlyNewerProjectionVersions() {
        UserRepository userRepository = mock(UserRepository.class);
        User user = User.createCustomer("identity", "name", "010", "user@example.test", LocalDateTime.now());
        when(userRepository.findByIdForUpdate(25L)).thenReturn(Optional.of(user));
        UserProjectionService service = new UserProjectionService(userRepository);

        service.applyDefaultAddress(25L, 301L, 4L);
        service.applyDefaultAddress(25L, 302L, 3L);
        service.applySubscriptionStatus(25L, SubscriptionStatusPolicy.ACTIVE, 1L);
        service.applySubscriptionStatus(25L, SubscriptionStatusPolicy.INACTIVE, 1L);

        assertThat(user.getDefaultAddressId()).isEqualTo(301L);
        assertThat(user.getDefaultAddressVersion()).isEqualTo(4L);
        assertThat(user.getSubscriptionStatus()).isEqualTo(SubscriptionStatusPolicy.ACTIVE);
        assertThat(user.getSubscriptionVersion()).isEqualTo(1L);
    }
}
