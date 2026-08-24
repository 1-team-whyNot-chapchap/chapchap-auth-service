package com.chapchap.auth.domain.audit.response;

import com.chapchap.auth.domain.audit.constant.AuditActionType;
import com.chapchap.auth.domain.audit.constant.AuditResult;
import com.chapchap.auth.domain.audit.constant.AuditTargetType;
import com.chapchap.auth.domain.audit.entity.AuditLog;

import java.time.LocalDateTime;
import java.util.Map;

public record AuditLogResponse(
        Long actorUserId,
        AuditActionType actionType,
        AuditTargetType targetType,
        Long targetId,
        AuditResult result,
        String traceId,
        Map<String, Object> detail,
        LocalDateTime createdAt
) {
    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getActorUserId(),
                auditLog.getActionType(),
                auditLog.getTargetType(),
                auditLog.getTargetId(),
                auditLog.getResult(),
                auditLog.getTraceId(),
                auditLog.getDetail(),
                auditLog.getCreatedAt()
        );
    }
}
