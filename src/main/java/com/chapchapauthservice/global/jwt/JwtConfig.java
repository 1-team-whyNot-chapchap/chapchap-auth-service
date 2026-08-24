package com.chapchapauthservice.global.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtConfig(
        boolean secure,
        String issuer,
        String type,

        // 일반 사용자 액세스 토큰 만료 시간(밀리초)
        int userAccessTokenExpiry,

        // 관리자 액세스 토큰 만료 시간(밀리초)
        int adminAccessTokenExpiry,

        String refreshTokenCookieName,

        // 일반 사용자 리프레시 쿠키 유지 시간(초)
        int userRefreshTokenCookieExpiry,

        // 관리자 리프레시 쿠키 유지 시간(초)
        int adminRefreshTokenCookieExpiry,

        String secret,
        String headerKey,
        String scheme,
        // Refresh Token 쿠키를 사용할 Auth API 범위
        String refreshTokenCookiePath
) {
}