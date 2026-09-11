package com.chapchap.auth.domain.audit.repository;

import com.chapchap.auth.domain.audit.constant.AuditActionType;
import com.chapchap.auth.domain.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    Page<AuditLog> findAllByActionTypeIn(Collection<AuditActionType> actionTypes, Pageable pageable);
}
