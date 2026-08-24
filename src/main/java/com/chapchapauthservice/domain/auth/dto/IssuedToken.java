package com.chapchapauthservice.domain.auth.dto;

import com.chapchapauthservice.global.security.constant.SessionTypePolicy;

public record IssuedToken(
    String accessToken,
    String refreshToken,
    SessionTypePolicy sessionType
) {
}
