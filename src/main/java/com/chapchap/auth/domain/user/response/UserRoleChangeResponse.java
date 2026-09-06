package com.chapchap.auth.domain.user.response;

import com.chapchap.auth.global.security.constant.RolePolicy;

public record UserRoleChangeResponse(Long userId, RolePolicy previousRole, RolePolicy newRole) {
}
