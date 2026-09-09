package com.chapchap.auth.domain.user.request;

import com.chapchap.auth.global.security.constant.RolePolicy;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserRoleChangeRequest(
        @NotNull
        @Schema(description = "라이더 승격은 RIDER, 고객 역할 복귀는 CUSTOMER",
                allowableValues = {"CUSTOMER", "RIDER"}, example = "RIDER")
        RolePolicy targetRole
) {
}
