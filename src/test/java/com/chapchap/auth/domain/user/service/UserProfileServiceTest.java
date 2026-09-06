package com.chapchap.auth.domain.user.service;

import com.chapchap.auth.domain.policy.repository.UserPolicyConsentRepository;
import com.chapchap.auth.domain.audit.service.AuditLogService;
import com.chapchap.auth.domain.token.repository.AuthSessionRepository;
import com.chapchap.auth.domain.user.entity.User;
import com.chapchap.auth.domain.user.repository.SocialAccountRepository;
import com.chapchap.auth.domain.user.repository.UserRepository;
import com.chapchap.auth.global.minio.MinioManager;
import com.chapchap.auth.global.kafka.producer.AuthEventProducer;
import com.chapchap.auth.global.error.custom.business.InvalidStateException;
import com.chapchap.auth.global.security.constant.SubscriptionStatusPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UserProfileServiceTest {

    @Test
    void cleansNewObjectAndKeepsPreviousKeyWhenDatabaseUpdateFails() {
        UserRepository userRepository = mock(UserRepository.class);
        SocialAccountRepository socialAccountRepository = mock(SocialAccountRepository.class);
        MinioManager minioManager = mock(MinioManager.class);
        User user = User.createCustomer("identity-key", "name", "010", "user@example.test", LocalDateTime.now());
        user.changeProfileImageKey("profiles/old.png");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(minioManager.generateProfileImageObjectKey(any())).thenReturn("profiles/new.png");
        doThrow(new DataIntegrityViolationException("db failure")).when(userRepository).saveAndFlush(user);
        UserProfileService service = new UserProfileService(
                userRepository,
                socialAccountRepository,
                mock(AuthSessionRepository.class),
                mock(UserPolicyConsentRepository.class),
                minioManager,
                mock(AuthEventProducer.class),
                mock(AuditLogService.class)
        );

        assertThatThrownBy(() -> service.replaceProfileImage(
                1L,
                new MockMultipartFile("file", "profile.png", "image/png", new byte[]{1})
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(user.getProfileImageKey()).isEqualTo("profiles/old.png");
        verify(minioManager).uploadFile(eq("profiles/new.png"), any());
        verify(minioManager).deleteFile("profiles/new.png");
        verify(minioManager, never()).deleteFile("profiles/old.png");
    }

    @Test
    void recordsAuditAndSchedulesWithdrawalEventAfterSuccessfulWithdrawal() {
        UserRepository userRepository = mock(UserRepository.class);
        SocialAccountRepository socialAccountRepository = mock(SocialAccountRepository.class);
        AuthSessionRepository authSessionRepository = mock(AuthSessionRepository.class);
        UserPolicyConsentRepository consentRepository = mock(UserPolicyConsentRepository.class);
        AuthEventProducer authEventProducer = mock(AuthEventProducer.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        User user = User.createCustomer("identity-key", "name", "010", "user@example.test", LocalDateTime.now());
        when(userRepository.findById(25L)).thenReturn(Optional.of(user));
        when(authSessionRepository.findAllByUser(user)).thenReturn(java.util.List.of());
        when(consentRepository.findAllByUser(user)).thenReturn(java.util.List.of());
        UserProfileService service = new UserProfileService(
                userRepository,
                socialAccountRepository,
                authSessionRepository,
                consentRepository,
                mock(MinioManager.class),
                authEventProducer,
                auditLogService
        );

        service.withdraw(25L);

        verify(auditLogService).recordUserWithdrawal(25L);
        verify(authEventProducer).publishUserWithdrawnAfterCommit(eq(25L), eq(user.getWithdrawnAt()));
    }

    @Test
    void rejectsWithdrawalWhenSubscriptionProjectionIsActiveOrUnknown() {
        UserRepository userRepository = mock(UserRepository.class);
        User user = User.createCustomer("active", "name", "010", "active@example.test", LocalDateTime.now());
        user.updateSubscriptionProjection(SubscriptionStatusPolicy.ACTIVE, 1L);
        when(userRepository.findById(25L)).thenReturn(Optional.of(user));
        UserProfileService service = new UserProfileService(
                userRepository, mock(SocialAccountRepository.class), mock(AuthSessionRepository.class),
                mock(UserPolicyConsentRepository.class), mock(MinioManager.class),
                mock(AuthEventProducer.class), mock(AuditLogService.class)
        );

        assertThatThrownBy(() -> service.withdraw(25L)).isInstanceOf(InvalidStateException.class);
        user.updateSubscriptionProjection(SubscriptionStatusPolicy.UNKNOWN, 2L);

        assertThatThrownBy(() -> service.withdraw(25L)).isInstanceOf(InvalidStateException.class);
    }
}
