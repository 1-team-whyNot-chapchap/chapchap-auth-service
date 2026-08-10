package com.chapchapauthservice.global.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtConfig(
        boolean secure,
        String issuer,
        String type,

        // 액세스 토큰 만료 시간(밀리초)
        int accessTokenExpiry,

        // 일반 사용자와 관리자의 리프레시 토큰 만료 시간(밀리초)
        int userRefreshTokenExpiry,
        int adminRefreshTokenExpiry,

        String refreshTokenCookieName,

        // 일반 사용자와 관리자의 리프레시 쿠키 유지 시간(초)
        int userRefreshTokenCookieExpiry,
        int adminRefreshTokenCookieExpiry,

        String secret,
        String headerKey,
        String scheme,
        String reissueUri
) {
}