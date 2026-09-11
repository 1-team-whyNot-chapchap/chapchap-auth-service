package com.chapchap.auth.domain.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

@Service
public class GoogleOidcService implements OAuth2UserService<OidcUserRequest, OidcUser> {
    private final GoogleOAuth2Service accountService;
    private final OAuth2UserService<OidcUserRequest, OidcUser> delegate;

    @Autowired
    public GoogleOidcService(GoogleOAuth2Service accountService) {
        this(accountService, new OidcUserService());
    }

    GoogleOidcService(GoogleOAuth2Service accountService,
                      OAuth2UserService<OidcUserRequest, OidcUser> delegate) {
        this.accountService = accountService;
        this.delegate = delegate;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest request) {
        if (!"google".equals(request.getClientRegistration().getRegistrationId())) {
            throw new OAuth2AuthenticationException(new OAuth2Error("unsupported_provider"));
        }
        // Spring's OIDC authentication provider validates the ID token before this call.
        // The standard user service retains UserInfo/subject validation as well.
        OidcUser verified = delegate.loadUser(request);
        return new ApplicationOidcUser(verified, accountService.resolvePrincipal(verified));
    }

    private static final class ApplicationOidcUser implements OidcUser {
        private final OidcUser verified;
        private final OAuth2User application;

        private ApplicationOidcUser(OidcUser verified, OAuth2User application) {
            this.verified = verified;
            this.application = application;
        }

        @Override public Map<String, Object> getClaims() { return verified.getClaims(); }
        @Override public OidcIdToken getIdToken() { return verified.getIdToken(); }
        @Override public OidcUserInfo getUserInfo() { return verified.getUserInfo(); }
        @Override public Map<String, Object> getAttributes() { return application.getAttributes(); }
        @Override public Collection<? extends GrantedAuthority> getAuthorities() { return application.getAuthorities(); }
        @Override public String getName() { return application.getName(); }
    }
}
