package com.chapchap.auth.domain.admin.response;

public record AdminLoginResponse(
        String accessToken,
        boolean mustChangePassword
) {
}
