package com.chapchap.auth.domain.auth.service.oauth2;

import com.chapchap.auth.domain.auth.service.GoogleOAuth2Service;
import com.chapchap.auth.domain.auth.service.KakaoOAuth2Service;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DelegatingOAuth2UserServiceTest {

    @Test
    void routesGoogleRequestToGoogleOAuth2Service() {
        KakaoOAuth2Service kakaoOAuth2Service = mock(KakaoOAuth2Service.class);
        GoogleOAuth2Service googleOAuth2Service = mock(GoogleOAuth2Service.class);
        DelegatingOAuth2UserService service = new DelegatingOAuth2UserService(kakaoOAuth2Service, googleOAuth2Service);
        OAuth2User expected = new DefaultOAuth2User(java.util.List.of(), Map.of("sub", "google-user"), "sub");
        OAuth2UserRequest request = requestFor("google");
        when(googleOAuth2Service.loadUser(request)).thenReturn(expected);

        OAuth2User actual = service.loadUser(request);

        assertThat(actual).isSameAs(expected);
        verify(googleOAuth2Service).loadUser(request);
        verifyNoInteractions(kakaoOAuth2Service);
    }

    private OAuth2UserRequest requestFor(String registrationId) {
        ClientRegistration registration = ClientRegistration.withRegistrationId(registrationId)
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://example.test/callback")
                .authorizationUri("https://example.test/authorize")
                .tokenUri("https://example.test/token")
                .userInfoUri("https://example.test/userinfo")
                .userNameAttributeName("sub")
                .clientName("Google")
                .build();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "access-token",
                Instant.now(),
                Instant.now().plusSeconds(60)
        );
        return new OAuth2UserRequest(registration, accessToken);
    }
}
