package com.chapchap.auth.global.security.filter;

import com.chapchap.auth.domain.admin.entity.AdminCredential;
import com.chapchap.auth.domain.admin.repository.AdminCredentialRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

class AdminPasswordChangeRequiredFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void blocksMustChangePasswordAdministratorFromAuditApi() throws Exception {
        AdminCredentialRepository repository = mock(AdminCredentialRepository.class);
        AdminCredential credential = mock(AdminCredential.class);
        when(credential.isMustChangePassword()).thenReturn(true);
        when(repository.findByUserId(25L)).thenReturn(Optional.of(credential));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "25", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        ));
        AdminPasswordChangeRequiredFilter filter = new AdminPasswordChangeRequiredFilter(repository, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/audit-logs");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> fail("mustChangePassword 관리자는 여기까지 도달하면 안 됩니다."));

        assertThat(response.getStatus()).isEqualTo(403);
    }
}
