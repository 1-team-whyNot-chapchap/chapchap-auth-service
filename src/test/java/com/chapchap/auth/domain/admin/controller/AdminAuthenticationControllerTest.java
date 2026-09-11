package com.chapchap.auth.domain.admin.controller;

import com.chapchap.auth.domain.admin.dto.AdminLoginResult;
import com.chapchap.auth.domain.admin.request.AdminLoginRequest;
import com.chapchap.auth.domain.admin.response.AdminLoginResponse;
import com.chapchap.auth.domain.admin.service.AdminAuthenticationService;
import com.chapchap.auth.global.service.cookie.CookieManager;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.mockito.Mockito.*;

class AdminAuthenticationControllerTest {
    @Test
    void initialPasswordLoginClearsAnyPreviousRefreshCookie() {
        var service = mock(AdminAuthenticationService.class);
        var cookies = mock(CookieManager.class);
        var request = new AdminLoginRequest("test-admin", "temporary-test-password");
        when(service.login(request)).thenReturn(new AdminLoginResult(new AdminLoginResponse(null, true), null));
        var response = new MockHttpServletResponse();
        new AdminAuthenticationController(service, cookies).login(request, response);
        verify(cookies).removeRefreshTokenToCookie(response);
        verifyNoMoreInteractions(cookies);
    }
}
