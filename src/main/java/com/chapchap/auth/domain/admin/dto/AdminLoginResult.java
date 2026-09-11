package com.chapchap.auth.domain.admin.dto;

import com.chapchap.auth.domain.admin.response.AdminLoginResponse;
import com.chapchap.auth.domain.auth.dto.IssuedToken;

public record AdminLoginResult(
        AdminLoginResponse response,
        IssuedToken issuedToken
) {
}
