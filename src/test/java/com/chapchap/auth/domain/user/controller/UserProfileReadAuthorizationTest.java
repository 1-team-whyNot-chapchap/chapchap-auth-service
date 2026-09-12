package com.chapchap.auth.domain.user.controller;

import com.chapchap.auth.domain.user.service.UserProfileService;
import com.chapchap.auth.domain.user.response.UserProfileImageResponse;
import com.chapchap.auth.domain.policy.service.MarketingConsentService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class UserProfileReadAuthorizationTest {
    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class Security {}

    @Test
    void onlyGeneralUsersCanReadTheirOwnImageAndConsent() {
        try (var context = new AnnotationConfigApplicationContext()) {
            var profiles = mock(UserProfileService.class);
            var consents = mock(MarketingConsentService.class);
            context.register(Security.class);
            context.registerBean(UserProfileService.class, () -> profiles);
            context.registerBean(MarketingConsentService.class, () -> consents);
            context.registerBean(UserProfileController.class);
            context.refresh();
            var controller = context.getBean(UserProfileController.class);
            for (String role : new String[]{"CUSTOMER", "RIDER"}) {
                var actor = new UsernamePasswordAuthenticationToken("42", null, AuthorityUtils.createAuthorityList("ROLE_" + role));
                SecurityContextHolder.getContext().setAuthentication(actor);
                when(profiles.getProfileImage(42L)).thenReturn(new UserProfileImageResponse(new byte[]{1}, "image/png"));
                var response = controller.getProfileImage(actor);
                assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
                assertThat(response.getHeaders().getContentType().toString()).isEqualTo("image/png");
                controller.getMarketingConsent(actor, 3L);
            }
            verify(profiles, times(2)).getProfileImage(42L);
            verify(consents, times(2)).getCurrent(42L, 3L);
            for (String role : new String[]{"ADMIN", "SUPER_ADMIN", "ANONYMOUS"}) {
                var actor = new UsernamePasswordAuthenticationToken("42", null, AuthorityUtils.createAuthorityList("ROLE_" + role));
                SecurityContextHolder.getContext().setAuthentication(actor);
                assertThatThrownBy(() -> controller.getProfileImage(actor)).isInstanceOf(AccessDeniedException.class);
                assertThatThrownBy(() -> controller.getMarketingConsent(actor, 3L)).isInstanceOf(AccessDeniedException.class);
            }
            verifyNoMoreInteractions(profiles, consents);
        } finally { SecurityContextHolder.clearContext(); }
    }
}
