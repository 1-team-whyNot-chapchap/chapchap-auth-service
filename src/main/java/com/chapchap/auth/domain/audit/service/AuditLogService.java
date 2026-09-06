package com.chapchap.auth.domain.audit.service;

import com.chapchap.auth.domain.audit.constant.AuditActionType;
import com.chapchap.auth.domain.audit.constant.AuditResult;
import com.chapchap.auth.domain.audit.constant.AuditTargetType;
import com.chapchap.auth.domain.audit.entity.AuditLog;
import com.chapchap.auth.domain.audit.repository.AuditLogRepository;
import com.chapchap.auth.domain.audit.response.AuditLogResponse;
import com.chapchap.auth.global.security.constant.RolePolicy;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private static final Set<String> FORBIDDEN_DETAIL_KEYS = Set.of(
            "password", "passwordHash", "accessToken", "refreshToken", "oauthAccessToken",
            "oauthClientSecret", "portOneApiSecret", "webhookSecret", "identityKey", "di"
    );
    private static final Set<AuditActionType> ADMIN_VISIBLE_ACTIONS = Set.of(
            AuditActionType.RIDER_ROLE_GRANTED,
            AuditActionType.RIDER_ROLE_REVOKED
    );
    private static final Map<AuditActionType, Set<String>> ALLOWED_DETAIL_KEYS = allowedDetailKeys();

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void recordUserWithdrawal(Long userId) {
        record(
                userId,
                AuditActionType.USER_WITHDRAWN,
                AuditTargetType.USER,
                userId,
                AuditResult.SUCCESS,
                Map.of("beforeStatus", "ACTIVE", "afterStatus", "WITHDRAWN")
        );
    }

    @Transactional
    public void recordRefreshTokenReuse(Long userId, Long sessionId) {
        record(
                userId,
                AuditActionType.TOKEN_REUSE_DETECTED,
                AuditTargetType.AUTH_SESSION,
                sessionId,
                AuditResult.BLOCKED,
                Map.of("sessionId", sessionId, "revokedReason", "REFRESH_TOKEN_REUSE")
        );
    }

    @Transactional
    public void recordAdminLoginSucceeded(Long userId) {
        record(userId, AuditActionType.ADMIN_LOGIN_SUCCEEDED, AuditTargetType.USER, userId, AuditResult.SUCCESS, null);
    }

    @Transactional
    public void recordAdminLoginFailed(Long userId, int failedCount) {
        record(userId, AuditActionType.ADMIN_LOGIN_FAILED, AuditTargetType.USER, userId, AuditResult.FAILURE,
                Map.of("reasonCode", "INVALID_CREDENTIALS", "failedCount", failedCount));
    }

    @Transactional
    public void recordAdminLocked(Long userId, int failedCount, LocalDateTime lockedUntil) {
        record(userId, AuditActionType.ADMIN_LOCKED, AuditTargetType.USER, userId, AuditResult.BLOCKED,
                Map.of("reasonCode", "LOGIN_FAILURE_LIMIT", "failedCount", failedCount, "lockedUntil", lockedUntil.toString()));
    }

    @Transactional
    public void recordAdminUnlocked(Long actorUserId, Long targetUserId, String reasonCode) {
        record(actorUserId, AuditActionType.ADMIN_UNLOCKED, AuditTargetType.USER, targetUserId, AuditResult.SUCCESS,
                Map.of("reasonCode", reasonCode));
    }

    @Transactional
    public void recordAdminCreated(Long actorUserId, Long targetUserId) {
        record(actorUserId, AuditActionType.ADMIN_CREATED, AuditTargetType.USER, targetUserId, AuditResult.SUCCESS,
                Map.of("createdRole", "ADMIN", "mustChangePassword", true));
    }

    @Transactional
    public void recordAdminPasswordChanged(Long actorUserId, Long targetUserId, String changeType) {
        record(actorUserId, AuditActionType.ADMIN_PASSWORD_CHANGED, AuditTargetType.USER, targetUserId, AuditResult.SUCCESS,
                Map.of("changeType", changeType));
    }

    @Transactional
    public void recordAdminPasswordReset(Long actorUserId, Long targetUserId) {
        record(actorUserId, AuditActionType.ADMIN_PASSWORD_RESET, AuditTargetType.USER, targetUserId, AuditResult.SUCCESS,
                Map.of("changeType", "SUPER_ADMIN_RESET", "mustChangePassword", true));
    }

    @Transactional
    public void recordAdminDisabled(Long actorUserId, Long targetUserId) {
        record(actorUserId, AuditActionType.ADMIN_DISABLED, AuditTargetType.USER, targetUserId, AuditResult.SUCCESS,
                Map.of("beforeStatus", "ACTIVE", "afterStatus", "SUSPENDED"));
    }

    @Transactional
    public void recordRiderRoleChanged(Long actorUserId, Long targetUserId, String previousRole, String newRole) {
        AuditActionType actionType = "RIDER".equals(newRole)
                ? AuditActionType.RIDER_ROLE_GRANTED
                : AuditActionType.RIDER_ROLE_REVOKED;
        record(actorUserId, actionType, AuditTargetType.USER, targetUserId, AuditResult.SUCCESS,
                Map.of("beforeRole", previousRole, "afterRole", newRole));
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(RolePolicy requesterRole, Pageable pageable) {
        Specification<AuditLog> specification;
        if (requesterRole == RolePolicy.SUPER_ADMIN) {
            specification = Specification.allOf();
        } else if (requesterRole == RolePolicy.ADMIN) {
            specification = (root, query, criteriaBuilder) -> root.get("actionType").in(ADMIN_VISIBLE_ACTIONS);
        } else {
            throw new AccessDeniedException("감사 로그를 조회할 권한이 없습니다.");
        }
        return auditLogRepository.findAll(specification, pageable).map(AuditLogResponse::from);
    }

    private void record(
            Long actorUserId,
            AuditActionType actionType,
            AuditTargetType targetType,
            Long targetId,
            AuditResult result,
            Map<String, Object> detail
    ) {
        validateDetail(actionType, detail);
        auditLogRepository.save(AuditLog.create(
                actorUserId,
                actionType,
                targetType,
                targetId,
                result,
                MDC.get("traceId"),
                detail
        ));
    }

    private void validateDetail(Map<String, Object> detail) {
        if (detail != null && detail.keySet().stream().anyMatch(FORBIDDEN_DETAIL_KEYS::contains)) {
            throw new IllegalStateException("감사 로그 detail에 금지된 민감값을 기록할 수 없습니다.");
        }
    }

    private void validateDetail(AuditActionType actionType, Map<String, Object> detail) {
        validateDetail(detail);
        Set<String> allowedKeys = ALLOWED_DETAIL_KEYS.get(actionType);
        if (detail != null && !allowedKeys.containsAll(detail.keySet())) {
            throw new IllegalStateException("감사 로그 actionType에 허용되지 않은 detail 키가 있습니다.");
        }
    }

    private static Map<AuditActionType, Set<String>> allowedDetailKeys() {
        Map<AuditActionType, Set<String>> detailKeys = new EnumMap<>(AuditActionType.class);
        detailKeys.put(AuditActionType.ADMIN_CREATED, Set.of("createdRole", "mustChangePassword"));
        detailKeys.put(AuditActionType.ADMIN_DISABLED, Set.of("beforeStatus", "afterStatus"));
        detailKeys.put(AuditActionType.ADMIN_PASSWORD_CHANGED, Set.of("changeType"));
        detailKeys.put(AuditActionType.ADMIN_PASSWORD_RESET, Set.of("changeType", "mustChangePassword"));
        detailKeys.put(AuditActionType.ADMIN_LOGIN_SUCCEEDED, Set.of());
        detailKeys.put(AuditActionType.ADMIN_LOGIN_FAILED, Set.of("reasonCode", "failedCount"));
        detailKeys.put(AuditActionType.ADMIN_LOCKED, Set.of("reasonCode", "failedCount", "lockedUntil"));
        detailKeys.put(AuditActionType.ADMIN_UNLOCKED, Set.of("reasonCode"));
        detailKeys.put(AuditActionType.RIDER_ROLE_GRANTED, Set.of("beforeRole", "afterRole"));
        detailKeys.put(AuditActionType.RIDER_ROLE_REVOKED, Set.of("beforeRole", "afterRole"));
        detailKeys.put(AuditActionType.USER_WITHDRAWN, Set.of("beforeStatus", "afterStatus"));
        detailKeys.put(AuditActionType.TOKEN_REUSE_DETECTED, Set.of("sessionId", "revokedReason"));
        return Map.copyOf(detailKeys);
    }
}
