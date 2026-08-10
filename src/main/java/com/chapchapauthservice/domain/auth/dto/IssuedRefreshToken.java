package com.chapchapauthservice.domain.auth.dto;

import com.chapchapauthservice.global.security.constant.SessionTypePolicy;

// 로그인 처리 후 SuccessHandler에 전달할 내부 결과 객체
// 쿠키에 저장할 리프레시 토큰 원문과 세션 종류를 함께 전달한다.
public record IssuedRefreshToken(
        String refreshToken,
        SessionTypePolicy sessionType
) {
}
