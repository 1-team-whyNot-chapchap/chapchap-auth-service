package com.chapchapauthservice.global.security.oauth2;

import com.chapchapauthserivce.domain.auth.repository.AuthRepository;
import com.chapchapauthserivce.domain.user.entity.User;
import com.chapchapauthserivce.global.config.SubServiceUriConfig;
import com.chapchapauthserivce.global.cookie.CookieManager;
import com.chapchapauthserivce.global.jwt.JwtProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final AuthRepository authRepository;
    private final JwtProvider jwtProvider;
    private final CookieManager cookieManager;
    private final SubServiceUriConfig subServiceUriConfig;

    @Override
    public void onAuthenticationSuccess(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Authentication authentication) throws IOException, ServletException {
        // OAuth2 로그인 성공 시 생성하여 시큐리티 세션에 담아둔 OAuth2User 객체 흭득
        DefaultOAuth2User oAuth2User = (DefaultOAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();
        Long userId = (Long) attributes.get("id");

        // 유저 조회
        User user = authRepository.findById(userId).orElseThrow();

        // 리프래시 토큰 DB 저장 처리
        String refreshToken = jwtProvider.generateRefreshToken(user);
        user.setRefreshToken(refreshToken);
        authRepository.save(user);

        // 리프래시 토큰 쿠키에 저장
        cookieManager.setRefreshTokenToCookie(response, refreshToken);

        this.getRedirectStrategy().sendRedirect(request, response, subServiceUriConfig.frontendCallbackUri());
    }
}
