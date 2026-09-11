package com.chapchap.auth.domain.auth.dto;

import com.chapchap.auth.domain.token.constant.SessionTypePolicy;

// 리프레시 토큰 재발급 결과
// 프론트에는 accessToken을 전달하고 refreshToken은 HttpOnly 쿠키에 다시 저장한다.
public record ReissuedToken(
        String accessToken,
        String refreshToken,
        SessionTypePolicy sessionType
) {
}
