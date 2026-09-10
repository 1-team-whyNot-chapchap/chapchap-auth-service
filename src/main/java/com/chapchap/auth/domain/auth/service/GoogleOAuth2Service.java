package com.chapchap.auth.domain.auth.service;

import com.chapchap.auth.domain.auth.entity.SignupSession;
import com.chapchap.auth.domain.auth.repository.SignupSessionRepository;
import com.chapchap.auth.domain.user.entity.SocialAccount;
import com.chapchap.auth.domain.user.entity.User;
import com.chapchap.auth.domain.user.repository.SocialAccountRepository;
import com.chapchap.auth.global.response.constant.CustomResponseCode;
import com.chapchap.auth.domain.auth.constant.ProviderPolicy;
import com.chapchap.auth.domain.user.constant.UserStatusPolicy;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GoogleOAuth2Service implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final SocialAccountRepository socialAccountRepository;
    private final SignupSessionRepository signupSessionRepository;

    @Override
    public OAuth2User loadUser(@NonNull OAuth2UserRequest request) {
        OAuth2User oAuthUser = new DefaultOAuth2UserService().loadUser(request);
        Object providerUserIdValue = oAuthUser.getAttributes().get("sub");

        if (providerUserIdValue == null) {
            throw createOAuth2Exception("Google 사용자 식별자를 가져올 수 없습니다.");
        }

        String providerUserId = String.valueOf(providerUserIdValue);
        Optional<SocialAccount> existingSocialAccount = socialAccountRepository
            .findByProviderAndProviderUserId(ProviderPolicy.GOOGLE, providerUserId);

        if (existingSocialAccount.isPresent()) {
            return createLoginPrincipal(existingSocialAccount.get());
        }

        SignupSession signupSession = signupSessionRepository.save(
            SignupSession.createPending(ProviderPolicy.GOOGLE, providerUserId)
        );

        return new DefaultOAuth2User(
            oAuthUser.getAuthorities(),
            Map.of(
                "authFlow", "SIGNUP",
                "signupSessionId", signupSession.getId(),
                "provider", signupSession.getProvider().name()
            ),
            "signupSessionId"
        );
    }

    private OAuth2User createLoginPrincipal(SocialAccount socialAccount) {
        User user = socialAccount.getUser();

        if (user.getStatus() != UserStatusPolicy.ACTIVE || user.getWithdrawnAt() != null) {
            throw createOAuth2Exception("현재 로그인할 수 없는 계정입니다.");
        }

        return new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
            Map.of(
                "authFlow", "LOGIN",
                "userId", user.getId(),
                "role", user.getRole().name()
            ),
            "userId"
        );
    }

    private OAuth2AuthenticationException createOAuth2Exception(String message) {
        return new OAuth2AuthenticationException(
            new OAuth2Error(CustomResponseCode.OAUTH2_ERROR.getCode(), message, null)
        );
    }
}
