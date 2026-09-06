package com.chapchap.auth.domain.audit.entity;

import com.chapchap.auth.domain.audit.constant.AuditActionType;
import com.chapchap.auth.domain.audit.constant.AuditResult;
import com.chapchap.auth.domain.audit.constant.AuditTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_log_id")
    private Long id;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private AuditActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 50)
    private AuditTargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 20)
    private AuditResult result;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail", columnDefinition = "json")
    private Map<String, Object> detail;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static AuditLog create(
            Long actorUserId,
            AuditActionType actionType,
            AuditTargetType targetType,
            Long targetId,
            AuditResult result,
            String traceId,
            Map<String, Object> detail
    ) {
        AuditLog auditLog = new AuditLog();
        auditLog.actorUserId = actorUserId;
        auditLog.actionType = actionType;
        auditLog.targetType = targetType;
        auditLog.targetId = targetId;
        auditLog.result = result;
        auditLog.traceId = traceId;
        auditLog.detail = detail == null || detail.isEmpty() ? null : Map.copyOf(detail);
        return auditLog;
    }
}
