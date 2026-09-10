package com.chapchap.auth.domain.auth.service.oauth2;

import com.chapchap.auth.domain.auth.dto.IssuedRefreshToken;
import com.chapchap.auth.domain.auth.service.AuthService;
import com.chapchap.auth.domain.user.entity.User;
import com.chapchap.auth.domain.user.repository.UserRepository;
import com.chapchap.auth.global.config.integration.SubServiceUriConfig;
import com.chapchap.auth.global.service.cookie.CookieManager;
import com.chapchap.auth.domain.token.constant.SessionTypePolicy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthService authService;

    @Mock
    private CookieManager cookieManager;

    @Mock
    private SubServiceUriConfig subServiceUriConfig;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void existingMemberLoginSetsOnlyRefreshCookieBeforeRedirect(boolean oidc) throws Exception {
        OAuth2SuccessHandler handler = new OAuth2SuccessHandler(
            userRepository,
            authService,
            cookieManager,
            subServiceUriConfig
        );
        User user = org.mockito.Mockito.mock(User.class);
        DefaultOAuth2User principal = new DefaultOAuth2User(
            List.of(),
            Map.of("authFlow", "LOGIN", "userId", 25L),
            "userId"
        );
        org.springframework.security.oauth2.core.user.OAuth2User authenticatedPrincipal = principal;
        if (oidc) {
            var oidcPrincipal = org.mockito.Mockito.mock(org.springframework.security.oauth2.core.oidc.user.OidcUser.class);
            when(oidcPrincipal.getAttributes()).thenReturn(principal.getAttributes());
            authenticatedPrincipal = oidcPrincipal;
        }
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            authenticatedPrincipal,
            null,
            principal.getAuthorities()
        );

        when(userRepository.findById(25L)).thenReturn(Optional.of(user));
        when(authService.issueInitialRefreshToken(user)).thenReturn(
            new IssuedRefreshToken("refresh-token", SessionTypePolicy.USER)
        );
        when(subServiceUriConfig.frontendCallbackUri()).thenReturn("https://frontend.example/callback");
        when(response.encodeRedirectURL("https://frontend.example/callback"))
            .thenReturn("https://frontend.example/callback");

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(cookieManager).setRefreshTokenToCookie(
            response,
            "refresh-token",
            SessionTypePolicy.USER
        );
        verify(response).sendRedirect("https://frontend.example/callback");
        verify(authService, never()).issueToken(any());
    }
}
