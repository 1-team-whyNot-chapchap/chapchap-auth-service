package com.chapchap.auth.domain.auth.service;

import com.chapchap.auth.domain.auth.constant.ProviderPolicy;
import com.chapchap.auth.domain.auth.entity.SignupSession;
import com.chapchap.auth.domain.auth.repository.SignupSessionRepository;
import com.chapchap.auth.domain.user.repository.SocialAccountRepository;
import com.chapchap.auth.domain.user.entity.SocialAccount;
import com.chapchap.auth.domain.user.entity.User;
import com.chapchap.auth.domain.user.constant.RolePolicy;
import com.chapchap.auth.domain.user.constant.UserStatusPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class GoogleOidcServiceTest {
    private final SocialAccountRepository accounts = mock(SocialAccountRepository.class);
    private final SignupSessionRepository signups = mock(SignupSessionRepository.class);
    private final GoogleOAuth2Service accountService = new GoogleOAuth2Service(accounts, signups);

    private OidcUserRequest request(String provider) {
        OidcUserRequest request = mock(OidcUserRequest.class);
        ClientRegistration registration = mock(ClientRegistration.class);
        when(request.getClientRegistration()).thenReturn(registration);
        when(registration.getRegistrationId()).thenReturn(provider);
        return request;
    }

    @Test
    void verifiedNewGoogleUserEntersSignupAndDoesNotTrustProviderRoutingClaims() {
        OidcUser verified = mock(OidcUser.class);
        when(verified.getAttributes()).thenReturn(Map.of("sub", "google-sub", "authFlow", "LOGIN", "userId", 999L));
        when(verified.getAuthorities()).thenReturn(List.of());
        when(accounts.findByProviderAndProviderUserId(ProviderPolicy.GOOGLE, "google-sub")).thenReturn(Optional.empty());
        when(signups.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        OidcUser result = new GoogleOidcService(accountService, ignored -> verified).loadUser(request("google"));
        assertThat(result.getAttributes()).containsEntry("authFlow", "SIGNUP").containsKey("signupSessionId").doesNotContainKey("userId");
        assertThat(result.getName()).isEqualTo(result.getAttributes().get("signupSessionId"));
        verify(signups).save(any(SignupSession.class));
    }

    @Test
    void oidcFailureCannotCreateSignupOrLogin() {
        GoogleOidcService service = new GoogleOidcService(accountService, ignored -> {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info_response"));
        });
        assertThatThrownBy(() -> service.loadUser(request("google"))).isInstanceOf(OAuth2AuthenticationException.class);
        verifyNoInteractions(accounts, signups);
    }

    @Test
    void unsupportedOidcProviderIsRejectedBeforeFetchingIdentity() {
        GoogleOidcService service = new GoogleOidcService(accountService, ignored -> { throw new AssertionError("Should not fetch"); });
        assertThatThrownBy(() -> service.loadUser(request("other"))).isInstanceOf(OAuth2AuthenticationException.class);
        verifyNoInteractions(accounts, signups);
    }

    @Test
    void invalidSubjectIsRejectedBeforeDatabaseAccess() {
        for (Object subject : List.of("", "  ", 123L)) {
            OidcUser verified = mock(OidcUser.class);
            when(verified.getAttributes()).thenReturn(Map.of("sub", subject));
            assertThatThrownBy(() -> new GoogleOidcService(accountService, ignored -> verified).loadUser(request("google")))
                    .isInstanceOf(OAuth2AuthenticationException.class);
        }
        verifyNoInteractions(accounts, signups);
    }

    @Test
    void existingUserUsesDatabaseRoleAndKeepsOidcToken() {
        OidcUser verified = mock(OidcUser.class);
        when(verified.getAttributes()).thenReturn(Map.of("sub", "existing", "role", "SUPER_ADMIN"));
        var token = mock(org.springframework.security.oauth2.core.oidc.OidcIdToken.class);
        when(verified.getIdToken()).thenReturn(token);
        User user = mock(User.class);
        when(user.getStatus()).thenReturn(UserStatusPolicy.ACTIVE);
        when(user.getRole()).thenReturn(RolePolicy.RIDER);
        when(user.getId()).thenReturn(25L);
        SocialAccount account = mock(SocialAccount.class);
        when(account.getUser()).thenReturn(user);
        when(accounts.findByProviderAndProviderUserId(ProviderPolicy.GOOGLE, "existing")).thenReturn(Optional.of(account));
        OidcUser result = new GoogleOidcService(accountService, ignored -> verified).loadUser(request("google"));
        assertThat(result.getAttributes()).containsEntry("authFlow", "LOGIN").containsEntry("userId", 25L).containsEntry("role", "RIDER");
        assertThat(result.getAuthorities()).extracting("authority").containsExactly("ROLE_RIDER");
        assertThat(result.getIdToken()).isSameAs(token);
        verifyNoInteractions(signups);
    }

    @Test
    void suspendedOrWithdrawnUserCannotLogin() {
        for (UserStatusPolicy status : List.of(UserStatusPolicy.SUSPENDED, UserStatusPolicy.WITHDRAWN, UserStatusPolicy.ACTIVE)) {
            OidcUser verified = mock(OidcUser.class);
            when(verified.getAttributes()).thenReturn(Map.of("sub", "inactive"));
            User user = mock(User.class);
            when(user.getStatus()).thenReturn(status);
            if (status == UserStatusPolicy.ACTIVE) when(user.getWithdrawnAt()).thenReturn(java.time.LocalDateTime.now());
            SocialAccount account = mock(SocialAccount.class);
            when(account.getUser()).thenReturn(user);
            when(accounts.findByProviderAndProviderUserId(ProviderPolicy.GOOGLE, "inactive")).thenReturn(Optional.of(account));
            assertThatThrownBy(() -> new GoogleOidcService(accountService, ignored -> verified).loadUser(request("google")))
                    .isInstanceOf(OAuth2AuthenticationException.class);
        }
        verifyNoInteractions(signups);
    }
}
