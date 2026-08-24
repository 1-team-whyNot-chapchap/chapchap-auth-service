package com.chapchap.auth.domain.user.service;

import com.chapchap.auth.domain.audit.service.AuditLogService;
import com.chapchap.auth.domain.token.service.AuthSessionService;
import com.chapchap.auth.domain.user.entity.User;
import com.chapchap.auth.domain.user.repository.UserRepository;
import com.chapchap.auth.domain.user.response.UserRoleChangeResponse;
import com.chapchap.auth.global.error.custom.business.NotFoundResourceException;
import com.chapchap.auth.global.error.custom.business.InvalidParameterException;
import com.chapchap.auth.global.error.custom.business.InvalidStateException;
import com.chapchap.auth.global.kafka.producer.AuthEventProducer;
import com.chapchap.auth.global.security.constant.RolePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserRoleService {
    private final UserRepository userRepository;
    private final AuthSessionService authSessionService;
    private final AuditLogService auditLogService;
    private final AuthEventProducer authEventProducer;

    @Transactional
    public UserRoleChangeResponse changeGeneralUserRole(Long actorUserId, Long targetUserId, RolePolicy targetRole) {
        if (targetRole != RolePolicy.CUSTOMER && targetRole != RolePolicy.RIDER) {
            throw new InvalidParameterException("CUSTOMER 또는 RIDER 역할만 지정할 수 있습니다.");
        }
        User user = userRepository.findByIdForUpdate(targetUserId)
                .orElseThrow(() -> new NotFoundResourceException("사용자를 찾을 수 없습니다."));
        RolePolicy previousRole = user.getRole();
        if (previousRole == targetRole) {
            throw new InvalidStateException("이미 요청한 역할입니다.");
        }
        user.changeGeneralRole(targetRole);
        authSessionService.revokeAllSessions(user);
        auditLogService.recordRiderRoleChanged(actorUserId, targetUserId, previousRole.name(), targetRole.name());
        authEventProducer.publishUserRoleChangedAfterCommit(targetUserId, previousRole, targetRole);
        return new UserRoleChangeResponse(targetUserId, previousRole, targetRole);
    }
}
