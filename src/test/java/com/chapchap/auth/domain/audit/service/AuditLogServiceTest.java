package com.chapchap.auth.domain.audit.service;

import com.chapchap.auth.domain.audit.constant.AuditActionType;
import com.chapchap.auth.domain.audit.constant.AuditResult;
import com.chapchap.auth.domain.audit.entity.AuditLog;
import com.chapchap.auth.domain.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuditLogServiceTest {

    @Test
    void recordsWithdrawalWithOnlyAllowedDetailFields() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        AuditLogService service = new AuditLogService(repository);

        service.recordUserWithdrawal(25L);

        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(logCaptor.capture());
        AuditLog auditLog = logCaptor.getValue();
        assertThat(auditLog.getActionType()).isEqualTo(AuditActionType.USER_WITHDRAWN);
        assertThat(auditLog.getResult()).isEqualTo(AuditResult.SUCCESS);
        assertThat(auditLog.getDetail()).containsExactlyInAnyOrderEntriesOf(
                java.util.Map.of("beforeStatus", "ACTIVE", "afterStatus", "WITHDRAWN")
        );
    }
}
