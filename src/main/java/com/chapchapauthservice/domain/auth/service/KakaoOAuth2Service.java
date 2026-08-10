package com.chapchapauthservice.domain.auth.service;

import com.chapchapauthservice.domain.user.entity.User;
import com.chapchapauthservice.domain.user.repository.UserRepository;
import com.chapchapauthservice.global.config.jpa.JPAWithDeleted;
import com.chapchapauthservice.global.response.constant.CustomResponseCode;
import com.chapchapauthservice.global.security.constant.ProviderPolicy;
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

@Service
@RequiredArgsConstructor
public class KakaoOAuth2Service implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    /*
     * Soft Delete된 회원도 조회해야 탈퇴 계정을 새로 만들지 않고 복구할 수 있다.
     * 카카오 응답의 중첩 Map 형변환 경고는 이 메서드 범위에서만 숨긴다.
     */
    @JPAWithDeleted
    @SuppressWarnings("unchecked")
    @Override
    public OAuth2User loadUser(@NonNull OAuth2UserRequest request) {
        // Spring Security 기본 구현체로 카카오에서 사용자 정보를 획득
        OAuth2User oAuthUser = new DefaultOAuth2UserService().loadUser(request);

        // 카카오 응답에서 가입·로그인에 필요한 정보 파싱
        Map<String, Object> attributes = oAuthUser.getAttributes();
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        String providerUserId = String.valueOf(attributes.get("id"));
        String email = (String) kakaoAccount.get("email");
        String nickname = (String) profile.get("nickname");
        String profileImageUrl = (String) profile.get("profile_image_url");

        // 닉네임은 users 테이블의 필수값이므로, 받지 못하면 로그인 실패 처리
        if (nickname == null || nickname.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(
                            CustomResponseCode.OAUTH2_ERROR.getCode(),
                            "카카오 닉네임 정보를 가져올 수 없습니다.",
                            null
                    )
            );
        }

        /*
         * provider + providerUserId 기준으로 회원 조회
         * 1. 회원이 없으면 소셜 회원가입
         * 2. 탈퇴 회원이면 기존 계정을 복구
         */
        User user = userRepository
                .findByProviderAndProviderUserId(ProviderPolicy.KAKAO, providerUserId)
                .orElseGet(() -> createKakaoUser(
                        providerUserId,
                        email,
                        nickname,
                        profileImageUrl
                ));

        // 같은 카카오 계정으로 재로그인한 탈퇴 회원은 기존 계정을 복구
        if (user.getDeletedAt() != null) {
            user.restore();
            userRepository.save(user);
        }

        /*
         * OAuth2 로그인 성공 후 SuccessHandler에 전달할 내부 회원 정보
         * 여기의 id는 카카오 ID가 아니라 우리 users 테이블의 user_id다.
         */
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                Map.of(
                        "id", user.getId(),
                        "role", user.getRole().name()
                ),
                "id"
        );
    }

    // 카카오에서 처음 로그인한 사용자를 CUSTOMER 권한으로 생성
    private User createKakaoUser(
            String providerUserId,
            String email,
            String nickname,
            String profileImageUrl
    ) {
        User user = User.createSocialUser(
                ProviderPolicy.KAKAO,
                providerUserId,
                email,
                nickname,
                profileImageUrl
        );

        return userRepository.save(user);
    }


}