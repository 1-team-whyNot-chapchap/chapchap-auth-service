package com.chapchap.auth.domain.admin.service;

import com.chapchap.auth.domain.admin.entity.AdminCredential;
import com.chapchap.auth.domain.admin.dto.AdminLoginResult;
import com.chapchap.auth.domain.admin.repository.AdminCredentialRepository;
import com.chapchap.auth.domain.admin.request.AdminLoginRequest;
import com.chapchap.auth.domain.admin.response.AdminLoginResponse;
import com.chapchap.auth.domain.audit.service.AuditLogService;
import com.chapchap.auth.domain.auth.dto.IssuedToken;
import com.chapchap.auth.domain.auth.service.AuthService;
import com.chapchap.auth.domain.user.entity.User;
import com.chapchap.auth.global.error.custom.business.InvalidCredentialException;
import com.chapchap.auth.domain.user.constant.RolePolicy;
import com.chapchap.auth.domain.user.constant.UserStatusPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminAuthenticationService {
    private final AdminCredentialRepository adminCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final AuditLogService auditLogService;

    @Transactional
    public AdminLoginResult login(AdminLoginRequest request) {
        AdminCredential credential = adminCredentialRepository.findByUsernameForUpdate(request.username())
                .orElseThrow(InvalidCredentialException::new);
        User user = credential.getUser();
        LocalDateTime now = LocalDateTime.now();

        if (credential.unlockIfExpired(now)) {
            auditLogService.recordAdminUnlocked(null, user.getId(), "LOCK_EXPIRED");
        }

        if (!isActiveAdministrator(user) || credential.getDisabledAt() != null || credential.isLockedAt(now)) {
            auditLogService.recordAdminLoginFailed(user.getId(), credential.getFailedLoginCount());
            throw new InvalidCredentialException();
        }

        if (!passwordEncoder.matches(request.password(), credential.getPasswordHash())) {
            boolean locked = credential.recordFailedLogin(now);
            auditLogService.recordAdminLoginFailed(user.getId(), credential.getFailedLoginCount());
            if (locked) {
                auditLogService.recordAdminLocked(user.getId(), credential.getFailedLoginCount(), credential.getLockedUntil());
            }
            throw new InvalidCredentialException();
        }

        credential.recordSuccessfulLogin(now);
        auditLogService.recordAdminLoginSucceeded(user.getId());
        if (credential.isMustChangePassword()) {
            return new AdminLoginResult(new AdminLoginResponse(null, true), null);
        }

        IssuedToken issuedToken = authService.issueToken(user);
        return new AdminLoginResult(new AdminLoginResponse(issuedToken.accessToken(), false), issuedToken);
    }

    private boolean isActiveAdministrator(User user) {
        return user.getStatus() == UserStatusPolicy.ACTIVE
                && (user.getRole() == RolePolicy.ADMIN || user.getRole() == RolePolicy.SUPER_ADMIN);
    }
}
