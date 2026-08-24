package com.chapchap.auth.domain.auth.controller;

import com.chapchap.auth.domain.auth.dto.ReissuedToken;
import com.chapchap.auth.domain.auth.response.AccessTokenResponse;
import com.chapchap.auth.domain.auth.service.AuthService;
import com.chapchap.auth.domain.auth.service.SignupService;
import com.chapchap.auth.global.cookie.CookieManager;
import com.chapchap.auth.global.response.GlobalResponse;
import com.chapchap.auth.global.security.constant.SessionTypePolicy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private SignupService signupService;

    @Mock
    private CookieManager cookieManager;

    @Mock
    private AuthService authService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Test
    void reissueTokenReturnsAccessTokenAndReplacesRefreshCookie() {
        AuthController controller = new AuthController(
            signupService,
            cookieManager,
            authService
        );
        when(cookieManager.getRefreshTokenToCookie(request)).thenReturn(Optional.of("old-refresh-token"));
        when(authService.reissueRefreshToken("old-refresh-token")).thenReturn(
            new ReissuedToken("new-access-token", "new-refresh-token", SessionTypePolicy.USER)
        );

        ResponseEntity<GlobalResponse<AccessTokenResponse>> result = controller.reissueToken(
            request,
            response
        );

        assertEquals(200, result.getStatusCode().value());
        assertEquals("new-access-token", result.getBody().data().accessToken());
        verify(cookieManager).setRefreshTokenToCookie(
            response,
            "new-refresh-token",
            SessionTypePolicy.USER
        );
    }
}
