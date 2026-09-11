package com.chapchap.auth.domain.user.response;

import com.chapchap.auth.domain.user.constant.RolePolicy;
import com.chapchap.auth.domain.user.constant.UserStatusPolicy;
import io.swagger.v3.oas.annotations.media.Schema;

public record AdminUserResponse(
        @Schema(description = "정밀도 손실 없이 사용하는 사용자 ID", example = "25") String userId,
        String name,
        RolePolicy role,
        UserStatusPolicy status
) {
}
