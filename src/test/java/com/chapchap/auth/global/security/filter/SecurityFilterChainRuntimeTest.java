package com.chapchap.auth.global.security.filter;

import com.chapchap.auth.global.config.security.SecurityConfiguration;

import com.chapchap.auth.domain.auth.service.oauth2.DelegatingOAuth2UserService;
import com.chapchap.auth.domain.auth.service.oauth2.OAuth2FailerHandler;
import com.chapchap.auth.domain.auth.service.oauth2.OAuth2SuccessHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityFilterChainRuntimeTest {
    @Test
    void startsWithTraceOriginHeaderAndAdminChecksInOrder() {
        new WebApplicationContextRunner()
                .withUserConfiguration(SecurityConfiguration.class)
                .withBean(ApiAuthenticationEntryPoint.class,
                        () -> new ApiAuthenticationEntryPoint(new com.fasterxml.jackson.databind.ObjectMapper()))
                .withBean(HeaderAuthenticationFilter.class, () -> mock(HeaderAuthenticationFilter.class))
                .withBean(AdminPasswordChangeRequiredFilter.class,
                        () -> mock(AdminPasswordChangeRequiredFilter.class))
                .withBean(RefreshCookieOriginFilter.class, () -> mock(RefreshCookieOriginFilter.class))
                .withBean(TraceIdFilter.class, () -> mock(TraceIdFilter.class))
                .withBean(DelegatingOAuth2UserService.class, () -> mock(DelegatingOAuth2UserService.class))
                .withBean(OAuth2SuccessHandler.class, () -> mock(OAuth2SuccessHandler.class))
                .withBean(OAuth2FailerHandler.class, () -> mock(OAuth2FailerHandler.class))
                .withBean(ClientRegistrationRepository.class, () -> new InMemoryClientRegistrationRepository(
                        ClientRegistration.withRegistrationId("local-test")
                                .clientId("local-test")
                                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                                .redirectUri("http://localhost/callback")
                                .authorizationUri("https://example.invalid/authorize")
                                .tokenUri("https://example.invalid/token")
                                .userInfoUri("https://example.invalid/user")
                                .userNameAttributeName("id").build()))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var filters = context.getBean(SecurityFilterChain.class).getFilters();
                    assertThat(filters).containsSubsequence(
                            context.getBean(TraceIdFilter.class),
                            context.getBean(RefreshCookieOriginFilter.class),
                            context.getBean(HeaderAuthenticationFilter.class),
                            context.getBean(AdminPasswordChangeRequiredFilter.class));
                });
    }
}
