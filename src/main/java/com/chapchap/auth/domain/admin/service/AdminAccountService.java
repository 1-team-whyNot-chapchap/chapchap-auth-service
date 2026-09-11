package com.chapchap.auth.domain.admin.service;

import com.chapchap.auth.domain.admin.entity.AdminCredential;
import com.chapchap.auth.domain.admin.repository.AdminCredentialRepository;
import com.chapchap.auth.domain.admin.request.AdminAccountCreateRequest;
import com.chapchap.auth.domain.admin.request.AdminPasswordChangeRequest;
import com.chapchap.auth.domain.admin.request.AdminPasswordResetRequest;
import com.chapchap.auth.domain.admin.request.InitialAdminPasswordChangeRequest;
import com.chapchap.auth.domain.admin.response.AdminAccountResponse;
import com.chapchap.auth.domain.audit.service.AuditLogService;
import com.chapchap.auth.domain.token.service.AuthSessionService;
import com.chapchap.auth.domain.user.entity.User;
import com.chapchap.auth.domain.user.repository.UserRepository;
import com.chapchap.auth.global.error.custom.business.InvalidCredentialException;
import com.chapchap.auth.global.error.custom.business.DuplicatedResourceException;
import com.chapchap.auth.global.error.custom.business.NotFoundResourceException;
import com.chapchap.auth.global.error.custom.business.InvalidStateException;
import com.chapchap.auth.global.messaging.kafka.producer.AuthEventProducer;
import com.chapchap.auth.domain.user.constant.RolePolicy;
import com.chapchap.auth.domain.user.constant.UserStatusPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAccountService {
    private final AdminCredentialRepository adminCredentialRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminPasswordPolicy adminPasswordPolicy;
    private final AuthSessionService authSessionService;
    private final AuditLogService auditLogService;
    private final AuthEventProducer authEventProducer;

    @Transactional
    public void changeOwnPassword(Long userId, AdminPasswordChangeRequest request) {
        AdminCredential credential = getAdministratorCredential(userId);
        changePassword(credential, request.currentPassword(), request.newPassword(), request.newPasswordConfirmation());
        authSessionService.revokeAllSessions(credential.getUser());
        auditLogService.recordAdminPasswordChanged(userId, userId, "SELF_CHANGE");
    }

    @Transactional
    public void changeInitialPassword(InitialAdminPasswordChangeRequest request) {
        AdminCredential credential = adminCredentialRepository.findByUsernameForUpdate(request.username())
                .orElseThrow(InvalidCredentialException::new);
        if (!credential.isMustChangePassword()) {
            throw new InvalidCredentialException();
        }
        changePassword(credential, request.currentPassword(), request.newPassword(), request.newPasswordConfirmation());
        authSessionService.revokeAllSessions(credential.getUser());
        auditLogService.recordAdminPasswordChanged(credential.getUser().getId(), credential.getUser().getId(), "SELF_CHANGE");
    }

    @Transactional
    public AdminAccountResponse createAdmin(Long actorUserId, AdminAccountCreateRequest request) {
        if (adminCredentialRepository.existsByUsername(request.username())) {
            throw new DuplicatedResourceException("이미 사용 중인 관리자 아이디입니다.");
        }
        adminPasswordPolicy.validate(request.temporaryPassword(), request.temporaryPassword());
        User user = userRepository.save(User.createAdministrator(request.name(), RolePolicy.ADMIN));
        AdminCredential credential = adminCredentialRepository.save(AdminCredential.create(
                user,
                request.username(),
                passwordEncoder.encode(request.temporaryPassword()),
                true
        ));
        auditLogService.recordAdminCreated(actorUserId, user.getId());
        return toResponse(credential);
    }

    @Transactional
    public void resetPassword(Long actorUserId, Long targetUserId, AdminPasswordResetRequest request) {
        ensureDifferentActor(actorUserId, targetUserId);
        AdminCredential credential = getOperableAdminCredential(targetUserId);
        adminPasswordPolicy.validate(request.temporaryPassword(), request.temporaryPassword());
        credential.changePassword(passwordEncoder.encode(request.temporaryPassword()), true);
        authSessionService.revokeAllSessions(credential.getUser());
        auditLogService.recordAdminPasswordReset(actorUserId, targetUserId);
    }

    @Transactional
    public void disableAdmin(Long actorUserId, Long targetUserId) {
        ensureDifferentActor(actorUserId, targetUserId);
        AdminCredential credential = getOperableAdminCredential(targetUserId);
        User user = credential.getUser();
        user.suspendAdministrator();
        credential.disable();
        authSessionService.revokeAllSessions(user);
        auditLogService.recordAdminDisabled(actorUserId, targetUserId);
        authEventProducer.publishAdminAccountDisabledAfterCommit(targetUserId, credential.getDisabledAt());
    }

    @Transactional
    public void unlockAdmin(Long actorUserId, Long targetUserId) {
        ensureDifferentActor(actorUserId, targetUserId);
        AdminCredential credential = getOperableAdminCredential(targetUserId);
        credential.unlockManually();
        auditLogService.recordAdminUnlocked(actorUserId, targetUserId, "SUPER_ADMIN_MANUAL_UNLOCK");
    }

    private void changePassword(
            AdminCredential credential,
            String currentPassword,
            String newPassword,
            String confirmation
    ) {
        if (credential.getDisabledAt() != null
                || credential.getUser().getStatus() != UserStatusPolicy.ACTIVE
                || !passwordEncoder.matches(currentPassword, credential.getPasswordHash())) {
            throw new InvalidCredentialException();
        }
        adminPasswordPolicy.validate(newPassword, confirmation);
        if (passwordEncoder.matches(newPassword, credential.getPasswordHash())) {
            throw new InvalidStateException("현재 비밀번호와 같은 비밀번호로 변경할 수 없습니다.");
        }
        credential.changePassword(passwordEncoder.encode(newPassword), false);
    }

    private AdminCredential getAdministratorCredential(Long userId) {
        AdminCredential credential = adminCredentialRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new NotFoundResourceException("관리자 계정을 찾을 수 없습니다."));
        User user = credential.getUser();
        if (user.getRole() != RolePolicy.ADMIN && user.getRole() != RolePolicy.SUPER_ADMIN) {
            throw new InvalidStateException("관리자 계정만 처리할 수 있습니다.");
        }
        return credential;
    }

    private AdminCredential getOperableAdminCredential(Long userId) {
        AdminCredential credential = getAdministratorCredential(userId);
        if (credential.getUser().getRole() != RolePolicy.ADMIN
                || credential.getUser().getStatus() != UserStatusPolicy.ACTIVE
                || credential.getDisabledAt() != null) {
            throw new InvalidStateException("SUPER_ADMIN 계정에는 이 작업을 수행할 수 없습니다.");
        }
        return credential;
    }

    private void ensureDifferentActor(Long actorUserId, Long targetUserId) {
        if (actorUserId.equals(targetUserId)) {
            throw new InvalidStateException("자기 자신의 관리자 계정에는 이 작업을 수행할 수 없습니다.");
        }
    }

    private AdminAccountResponse toResponse(AdminCredential credential) {
        User user = credential.getUser();
        return new AdminAccountResponse(user.getId(), credential.getUsername(), user.getRole(), user.getStatus(), credential.isMustChangePassword());
    }
}
