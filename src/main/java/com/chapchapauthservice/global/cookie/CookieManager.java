package com.chapchapauthservice.global.cookie;

import com.chapchapauthservice.global.jwt.JwtConfig;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CookieManager {
    private final JwtConfig jwtConfig;

    public void setRefreshTokenToCookie(HttpServletResponse response, String refreshToken) {
        this.setCookie(
            response,
            jwtConfig.refreshTokenCookieName(),
            refreshToken,
            jwtConfig.refreshTokenCookieExpiry(),
            jwtConfig.reissueUri()
        );
    }

    public void removeRefreshTokenToCookie(HttpServletResponse response) {
        this.setCookie(
            response,
            jwtConfig.refreshTokenCookieName(),
            null,
            0,
            jwtConfig.reissueUri()
        );
    }

    public Optional<String> getRefreshTokenToCookie(HttpServletRequest request) {
        return this.getCookie(
            request,
            jwtConfig.refreshTokenCookieName()).map(Cookie::getValue)
        ;
    }

    private Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        // 쿠키 존재 여부 확인
        if (request.getCookies() == null) {
            return Optional.empty();
        }

        // name에 맞는 쿠키 획득
        return Arrays.stream(request.getCookies())
            .filter(cookie -> cookie.getName().equals(name))
            .findFirst();
    }

    // 쿠키 생성 메소드
    private void setCookie(HttpServletResponse response, String name, String value, int maxAge, String path){
        Cookie cookie = new Cookie(name, value); // 해당 이름과 값으로 쿠키 인스턴스 생성
        cookie.setPath(path); // 쿠키를 사용할 path 설정
        cookie.setMaxAge(maxAge); // 쿠키 유효 시간 설정
        cookie.setHttpOnly(true); // HTTPOnly 설정 (XSS 공격 방지)
        cookie.setSecure(jwtConfig.secure()); // 시큐어설정 (MITM 공격 방지)

        response.addCookie(cookie);
    }



}
