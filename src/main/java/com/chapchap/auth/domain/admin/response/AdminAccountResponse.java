package com.chapchap.auth.domain.admin.response;

import com.chapchap.auth.domain.user.constant.RolePolicy;
import com.chapchap.auth.domain.user.constant.UserStatusPolicy;

public record AdminAccountResponse(
        Long userId,
        String username,
        RolePolicy role,
        UserStatusPolicy status,
        boolean mustChangePassword
) {
}
