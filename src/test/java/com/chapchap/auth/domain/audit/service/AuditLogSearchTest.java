package com.chapchap.auth.domain.audit.service;

import com.chapchap.auth.domain.audit.repository.AuditLogRepository;
import com.chapchap.auth.domain.user.constant.RolePolicy;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class AuditLogSearchTest {

    @Test
    void rejectsAuditLogSearchForGeneralUsers() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        AuditLogService service = new AuditLogService(repository);

        assertThatThrownBy(() -> service.search(RolePolicy.CUSTOMER, PageRequest.of(0, 20)))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void allowsAdminSearchOnlyThroughRestrictedRepositoryQuery() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .thenReturn(Page.empty());
        AuditLogService service = new AuditLogService(repository);

        service.search(RolePolicy.ADMIN, PageRequest.of(0, 20));

        verify(repository).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class));
    }
}
