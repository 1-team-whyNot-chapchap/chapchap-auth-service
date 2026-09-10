package com.chapchap.auth.domain.auth.dto;

import com.chapchap.auth.domain.token.constant.SessionTypePolicy;

public record IssuedToken(
    String accessToken,
    String refreshToken,
    SessionTypePolicy sessionType
) {
}
