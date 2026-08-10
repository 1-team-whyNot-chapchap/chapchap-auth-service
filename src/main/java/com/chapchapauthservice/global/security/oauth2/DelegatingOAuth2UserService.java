package com.chapchapauthservice.global.security.oauth2;

import com.chapchapauthserivce.domain.auth.service.KakaoAuth2Service;
import com.chapchapauthserivce.global.response.constant.CustomResponseCode;
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
    private final KakaoAuth2Service kakaoAuth2Service;

    @Override
    public @Nullable OAuth2User loadUser(@NonNull OAuth2UserRequest request) throws OAuth2AuthenticationException {
        // OAuth2의 registrationId 흭득
        String registrationId = request.getClientRegistration().getRegistrationId();
        return switch (registrationId) {
            case "kakao" -> kakaoAuth2Service.loadUser(request);
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
