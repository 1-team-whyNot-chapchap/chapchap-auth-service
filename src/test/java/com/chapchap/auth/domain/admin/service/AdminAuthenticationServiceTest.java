package com.chapchap.auth.domain.admin.service;

import com.chapchap.auth.domain.admin.entity.AdminCredential;
import com.chapchap.auth.domain.admin.repository.AdminCredentialRepository;
import com.chapchap.auth.domain.admin.request.AdminLoginRequest;
import com.chapchap.auth.domain.audit.service.AuditLogService;
import com.chapchap.auth.domain.auth.dto.IssuedToken;
import com.chapchap.auth.domain.auth.service.AuthService;
import com.chapchap.auth.domain.user.entity.User;
import com.chapchap.auth.global.error.custom.business.InvalidCredentialException;
import com.chapchap.auth.global.security.constant.RolePolicy;
import com.chapchap.auth.global.security.constant.SessionTypePolicy;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AdminAuthenticationServiceTest {

    @Test
    void blocksLoginAfterFiveFailuresEvenWhenPasswordBecomesCorrect() {
        AdminCredentialRepository credentialRepository = mock(AdminCredentialRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        User user = User.createAdministrator("admin", RolePolicy.ADMIN);
        AdminCredential credential = AdminCredential.create(user, "admin-id", "hash", false);
        when(credentialRepository.findByUsernameForUpdate("admin-id")).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches(anyString(), eq("hash"))).thenReturn(false);
        AdminAuthenticationService service = new AdminAuthenticationService(
                credentialRepository, passwordEncoder, mock(AuthService.class), auditLogService
        );

        for (int index = 0; index < 5; index++) {
            assertThatThrownBy(() -> service.login(new AdminLoginRequest("admin-id", "wrong-password")))
                    .isInstanceOf(InvalidCredentialException.class);
        }
        when(passwordEncoder.matches(anyString(), eq("hash"))).thenReturn(true);

        assertThatThrownBy(() -> service.login(new AdminLoginRequest("admin-id", "correct-password")))
                .isInstanceOf(InvalidCredentialException.class);
        verify(auditLogService).recordAdminLocked(any(), eq(5), any());
    }

    @Test
    void issuesAdminTokenOnlyWhenPasswordChangeIsNotRequired() {
        AdminCredentialRepository credentialRepository = mock(AdminCredentialRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuthService authService = mock(AuthService.class);
        User user = User.createAdministrator("admin", RolePolicy.ADMIN);
        AdminCredential credential = AdminCredential.create(user, "admin-id", "hash", false);
        when(credentialRepository.findByUsernameForUpdate("admin-id")).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("correct-password", "hash")).thenReturn(true);
        IssuedToken issuedToken = new IssuedToken("access", "refresh", SessionTypePolicy.ADMIN);
        when(authService.issueToken(user)).thenReturn(issuedToken);
        AdminAuthenticationService service = new AdminAuthenticationService(
                credentialRepository, passwordEncoder, authService, mock(AuditLogService.class)
        );

        var result = service.login(new AdminLoginRequest("admin-id", "correct-password"));

        assertThat(result.response().accessToken()).isEqualTo("access");
        assertThat(result.issuedToken()).isEqualTo(issuedToken);
    }
}
