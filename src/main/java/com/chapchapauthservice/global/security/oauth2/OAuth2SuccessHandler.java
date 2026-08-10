package com.chapchapauthservice.global.security.oauth2;

import com.chapchapauthservice.domain.auth.dto.IssuedRefreshToken;
import com.chapchapauthservice.domain.auth.service.AuthService;
import com.chapchapauthservice.domain.user.entity.User;
import com.chapchapauthservice.domain.user.repository.UserRepository;
import com.chapchapauthservice.global.config.SubServiceUriConfig;
import com.chapchapauthservice.global.cookie.CookieManager;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

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

        // KakaoOAuth2Service가 전달한 우리 서비스의 user_id를 가져온다
        DefaultOAuth2User oAuth2User = (DefaultOAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();
        Long userId = (Long) attributes.get("id");
        
        // 로그인 대상을 다시 조회한 뒤, 세션과 리프레시 토큰을 발급한다
        User user = userRepository.findById(userId)
                .orElseThrow();

        IssuedRefreshToken issuedRefreshToken = authService.issueRefreshToken(user);

        // 실제 리프레시 토큰을 HttpOnly 쿠키에만 저장한다
        // DB 에는 AuthService가 저장한 토큰 해시값만 존재한다.
        cookieManager.setRefreshTokenToCookie(
                response,
                issuedRefreshToken.refreshToken(),
                issuedRefreshToken.sessionType()
        );

        // 초큰 발급이 끝나면 프론트엔드 로그인 완료 화면으로 이동한다.
        getRedirectStrategy().sendRedirect(
                request,
                response,
                subServiceUriConfig.frontendCallbackUri()
        );
    }
}
