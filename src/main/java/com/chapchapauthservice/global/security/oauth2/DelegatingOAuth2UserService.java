package com.chapchapauthservice.global.security.oauth2;

import com.chapchapauthservice.domain.auth.service.KakaoOAuth2Service;
import com.chapchapauthservice.global.response.constant.CustomResponseCode;
import jakarta.annotation.Nullable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DelegatingOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    
    // 로그인 요청의 provider에 따라 실제 처리 서비스를 선택
    private final KakaoOAuth2Service kakaoOAuth2Service;

    @Override
    public @Nullable OAuth2User loadUser(@NonNull OAuth2UserRequest request) throws OAuth2AuthenticationException {
        // OAuth2의 registrationId 흭득
        String registrationId = request.getClientRegistration().getRegistrationId();

        // application.yaml에 등록된 kakao 로그인 요청을 KakaoOAuth2Service로 전달
        return switch (registrationId) {
            case "kakao" -> kakaoOAuth2Service.loadUser(request);
            
            // 아직 지원하지 않는 소셜 로그인은 명확하게 실패 처리
            default -> throw new OAuth2AuthenticationException(
                new OAuth2Error(
                    CustomResponseCode.UNSUPPORTED_PROVIDER_ERROR.getCode(),
                    CustomResponseCode.UNSUPPORTED_PROVIDER_ERROR.name(),
                    null
                )
            );
        };
    }
}
