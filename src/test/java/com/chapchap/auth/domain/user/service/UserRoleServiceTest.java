package com.chapchap.auth.domain.user.service;

import com.chapchap.auth.domain.audit.service.AuditLogService;
import com.chapchap.auth.domain.token.service.AuthSessionService;
import com.chapchap.auth.domain.user.entity.User;
import com.chapchap.auth.domain.user.repository.UserRepository;
import com.chapchap.auth.global.kafka.producer.AuthEventProducer;
import com.chapchap.auth.global.security.constant.RolePolicy;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UserRoleServiceTest {

    @Test
    void changesCustomerToRiderAndRevokesAllExistingSessions() {
        UserRepository userRepository = mock(UserRepository.class);
        AuthSessionService authSessionService = mock(AuthSessionService.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        AuthEventProducer authEventProducer = mock(AuthEventProducer.class);
        User user = User.createCustomer("identity", "name", "010", "user@example.test", LocalDateTime.now());
        when(userRepository.findByIdForUpdate(25L)).thenReturn(Optional.of(user));
        UserRoleService service = new UserRoleService(userRepository, authSessionService, auditLogService, authEventProducer);

        var response = service.changeGeneralUserRole(1L, 25L, RolePolicy.RIDER);

        assertThat(response.previousRole()).isEqualTo(RolePolicy.CUSTOMER);
        assertThat(response.newRole()).isEqualTo(RolePolicy.RIDER);
        verify(authSessionService).revokeAllSessions(user);
        verify(auditLogService).recordRiderRoleChanged(1L, 25L, "CUSTOMER", "RIDER");
        verify(authEventProducer).publishUserRoleChangedAfterCommit(25L, RolePolicy.CUSTOMER, RolePolicy.RIDER);
    }
}
