package com.chapchap.auth.domain.user.service;

import com.chapchap.auth.domain.audit.service.AuditLogService;
import com.chapchap.auth.domain.token.service.AuthSessionService;
import com.chapchap.auth.domain.user.entity.User;
import com.chapchap.auth.domain.user.repository.UserRepository;
import com.chapchap.auth.global.messaging.kafka.producer.AuthEventProducer;
import com.chapchap.auth.domain.user.constant.RolePolicy;
import com.chapchap.auth.global.error.custom.business.InvalidParameterException;
import com.chapchap.auth.global.error.custom.business.InvalidStateException;
import com.chapchap.auth.global.error.custom.business.NotFoundResourceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserRoleServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final AuthSessionService sessions = mock(AuthSessionService.class);
    private final AuditLogService audit = mock(AuditLogService.class);
    private final AuthEventProducer events = mock(AuthEventProducer.class);
    private final UserRoleService service = new UserRoleService(users, sessions, audit, events);

    @ParameterizedTest
    @EnumSource(value = RolePolicy.class, names = {"ADMIN", "SUPER_ADMIN"})
    @NullSource
    void rejectsInvalidTargetRolesBeforeLoadingOrChangingAUser(RolePolicy role) {
        assertThatThrownBy(() -> service.changeGeneralUserRole(1L, 25L, role))
                .isInstanceOf(InvalidParameterException.class);
        verifyNoInteractions(users, sessions, audit, events);
    }

    @Test
    void rejectsMissingUserWithoutSideEffects() {
        when(users.findByIdForUpdate(25L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.changeGeneralUserRole(1L, 25L, RolePolicy.RIDER))
                .isInstanceOf(NotFoundResourceException.class);
        verifyNoInteractions(sessions, audit, events);
    }

    @ParameterizedTest
    @EnumSource(value = RolePolicy.class, names = {"ADMIN", "SUPER_ADMIN"})
    void cannotTurnAnAdministratorIntoARider(RolePolicy currentRole) {
        User user = User.createAdministrator("admin", currentRole);
        when(users.findByIdForUpdate(25L)).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> service.changeGeneralUserRole(1L, 25L, RolePolicy.RIDER))
                .isInstanceOf(InvalidStateException.class);
        assertThat(user.getRole()).isEqualTo(currentRole);
        verifyNoInteractions(sessions, audit, events);
    }

    @Test
    void cannotPromoteAWithdrawnCustomer() {
        User user = customer();
        user.withdraw();
        when(users.findByIdForUpdate(25L)).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> service.changeGeneralUserRole(1L, 25L, RolePolicy.RIDER))
                .isInstanceOf(InvalidStateException.class);
        assertThat(user.getRole()).isEqualTo(RolePolicy.CUSTOMER);
        verifyNoInteractions(sessions, audit, events);
    }

    @Test
    void duplicatePromotionDoesNotRepeatRevocationAuditOrEvent() {
        User user = customer();
        when(users.findByIdForUpdate(25L)).thenReturn(Optional.of(user));
        service.changeGeneralUserRole(1L, 25L, RolePolicy.RIDER);
        assertThat(user.getRole()).isEqualTo(RolePolicy.RIDER);
        assertThatThrownBy(() -> service.changeGeneralUserRole(1L, 25L, RolePolicy.RIDER))
                .isInstanceOf(InvalidStateException.class);
        verify(sessions, times(1)).revokeAllSessions(user);
        verify(audit, times(1)).recordRiderRoleChanged(1L, 25L, "CUSTOMER", "RIDER");
        verify(events, times(1)).publishUserRoleChangedAfterCommit(25L, RolePolicy.CUSTOMER, RolePolicy.RIDER);
    }

    private User customer() {
        return User.createCustomer("identity", "name", "010", "user@example.test", LocalDateTime.now());
    }

    @Test
    void suspendedCustomerIsRejectedWithoutSideEffects() {
        User user = customer();
        org.springframework.test.util.ReflectionTestUtils.setField(user, "status",
                com.chapchap.auth.domain.user.constant.UserStatusPolicy.SUSPENDED);
        when(users.findByIdForUpdate(25L)).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> service.changeGeneralUserRole(1L, 25L, RolePolicy.RIDER))
                .isInstanceOf(InvalidStateException.class);
        assertThat(user.getRole()).isEqualTo(RolePolicy.CUSTOMER);
        verifyNoInteractions(sessions, audit, events);
    }

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
