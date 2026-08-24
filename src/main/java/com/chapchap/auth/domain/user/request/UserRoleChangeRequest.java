package com.chapchap.auth.domain.user.request;

import com.chapchap.auth.global.security.constant.RolePolicy;
import jakarta.validation.constraints.NotNull;

public record UserRoleChangeRequest(@NotNull RolePolicy targetRole) {
}
