package com.chapchap.auth.domain.auth.service;

import com.chapchap.auth.domain.auth.entity.SignupSession;
import com.chapchap.auth.domain.auth.repository.SignupSessionRepository;
import com.chapchap.auth.domain.user.entity.SocialAccount;
import com.chapchap.auth.domain.user.entity.User;
import com.chapchap.auth.domain.user.repository.SocialAccountRepository;
import com.chapchap.auth.global.response.constant.CustomResponseCode;
import com.chapchap.auth.global.security.constant.ProviderPolicy;
import com.chapchap.auth.global.security.constant.UserStatusPolicy;
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
public class KakaoOAuth2Service
    implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final SocialAccountRepository socialAccountRepository;
    private final SignupSessionRepository signupSessionRepository;

    @Override
    public OAuth2User loadUser(
        @NonNull OAuth2UserRequest request
    ) {

        // 카카오 Access Token으로 사용자 정보 조회
        OAuth2User oAuthUser =
            new DefaultOAuth2UserService()
                .loadUser(request);

        Map<String, Object> attributes =
            oAuthUser.getAttributes();

        // 카카오 사용자 고유 ID 확인
        Object providerUserIdValue =
            attributes.get("id");

        if (providerUserIdValue == null) {
            throw createOAuth2Exception(
                "카카오 사용자 식별자를 가져올 수 없습니다."
            );
        }

        String providerUserId =
            String.valueOf(providerUserIdValue);

        // 등록된 카카오 로그인 수단 조회
        Optional<SocialAccount> existingSocialAccount =
            socialAccountRepository
                .findByProviderAndProviderUserId(
                    ProviderPolicy.KAKAO,
                    providerUserId
                );

        // 이미 등록된 소셜 계정이면 정상 로그인 흐름
        if (existingSocialAccount.isPresent()) {
            return createLoginPrincipal(
                existingSocialAccount.get()
            );
        }

        // 미등록 소셜 계정은 User를 만들지 않고 가입 세션만 생성
        SignupSession signupSession =
            signupSessionRepository.save(
                SignupSession.createPending(
                    ProviderPolicy.KAKAO,
                    providerUserId
                )
            );

        return createSignupPrincipal(
            oAuthUser,
            signupSession
        );
    }


    // 기존 회원 로그인용 OAuth2 Principal 생성
    private OAuth2User createLoginPrincipal(
        SocialAccount socialAccount
    ) {

        User user = socialAccount.getUser();

        // ACTIVE 사용자만 Token 발급 단계로 이동 가능
        if (user.getStatus() != UserStatusPolicy.ACTIVE
                || user.getWithdrawnAt() != null) {

            throw createOAuth2Exception(
                "현재 로그인할 수 없는 계정입니다."
            );
        }

        return new DefaultOAuth2User(
            List.of(
                new SimpleGrantedAuthority(
                    "ROLE_" + user.getRole().name()
                )
            ),
            Map.of(
                "authFlow", "LOGIN",
                "userId", user.getId(),
                "role", user.getRole().name()
            ),
            "userId"
        );
    }


    // 신규 가입 진행용 OAuth2 Principal 생성
    private OAuth2User createSignupPrincipal(
        OAuth2User oAuthUser,
        SignupSession signupSession
    ) {

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


    // OAuth2 인증 과정의 비즈니스 실패를 Spring Security 예외로 변환
    private OAuth2AuthenticationException createOAuth2Exception(
        String message
    ) {
        return new OAuth2AuthenticationException(
            new OAuth2Error(
                CustomResponseCode.OAUTH2_ERROR.getCode(),
                message,
                null
            )
        );
    }
}