package com.chapchap.auth.domain.auth.service.oauth2;

import com.chapchap.auth.domain.auth.dto.IssuedRefreshToken;
import com.chapchap.auth.domain.auth.service.AuthService;
import com.chapchap.auth.domain.user.entity.User;
import com.chapchap.auth.domain.user.repository.UserRepository;
import com.chapchap.auth.global.config.integration.SubServiceUriConfig;
import com.chapchap.auth.global.service.cookie.CookieManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler
    extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final CookieManager cookieManager;
    private final SubServiceUriConfig subServiceUriConfig;

    @Override
    public void onAuthenticationSuccess(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull Authentication authentication
    ) throws IOException, ServletException {

        DefaultOAuth2User oAuth2User =
            (DefaultOAuth2User) authentication.getPrincipal();

        Map<String, Object> attributes =
            oAuth2User.getAttributes();

        String authFlow =
            String.valueOf(attributes.get("authFlow"));

        // 기존 회원 로그인
        if ("LOGIN".equals(authFlow)) {
            handleLogin(
                request,
                response,
                attributes
            );

            return;
        }

        // 신규 회원 가입 시작
        if ("SIGNUP".equals(authFlow)) {
            handleSignup(
                request,
                response,
                attributes
            );

            return;
        }

        throw new IllegalStateException(
            "지원하지 않는 OAuth2 인증 흐름입니다."
        );
    }


    // 기존 회원에게 인증 세션과 Refresh Token 발급
    private void handleLogin(
        HttpServletRequest request,
        HttpServletResponse response,
        Map<String, Object> attributes
    ) throws IOException {

        Object userIdValue =
            attributes.get("userId");

        if (!(userIdValue instanceof Number number)) {
            throw new IllegalStateException(
                "OAuth2 로그인 사용자 ID가 올바르지 않습니다."
            );
        }

        Long userId = number.longValue();

        User user = userRepository.findById(userId)
                        .orElseThrow(() ->
                                         new IllegalStateException(
                                             "로그인 사용자를 찾을 수 없습니다."
                                         )
                        );

        IssuedRefreshToken issuedRefreshToken =
            authService.issueInitialRefreshToken(user);

        // Access Token은 redirect URL에 노출하지 않는다.
        // Refresh Token 원문만 HttpOnly Cookie로 전달하고 callback 이후
        // /api/auth/reissue-token에서 Access Token을 응답 본문으로 받는다.
        cookieManager.setRefreshTokenToCookie(
            response,
            issuedRefreshToken.refreshToken(),
            issuedRefreshToken.sessionType()
        );

        getRedirectStrategy().sendRedirect(
            request,
            response,
            subServiceUriConfig.frontendCallbackUri()
        );
    }


    // 신규 회원에게 Token 대신 signupSessionId만 전달
    private void handleSignup(
        HttpServletRequest request,
        HttpServletResponse response,
        Map<String, Object> attributes
    ) throws IOException {

        Object signupSessionIdValue =
            attributes.get("signupSessionId");

        if (signupSessionIdValue == null) {
            throw new IllegalStateException(
                "가입 세션 ID가 존재하지 않습니다."
            );
        }

        String signupSessionId =
            String.valueOf(signupSessionIdValue);

        String redirectUri =
            UriComponentsBuilder
                .fromUriString(
                    subServiceUriConfig.frontendCallbackUri()
                )
                .queryParam(
                    "signupSessionId",
                    signupSessionId
                )
                .build()
                .encode()
                .toUriString();

        // 신규 가입자는 Access/Refresh Token을 발급하지 않는다.
        getRedirectStrategy().sendRedirect(
            request,
            response,
            redirectUri
        );
    }
}
