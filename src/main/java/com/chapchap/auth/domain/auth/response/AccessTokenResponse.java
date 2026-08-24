package com.chapchap.auth.domain.auth.response;

// 인증 성공 후 클라이언트에 전달하는 Access Token 응답
public record AccessTokenResponse(
    String accessToken
) {
}
