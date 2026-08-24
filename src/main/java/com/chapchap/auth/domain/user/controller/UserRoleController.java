package com.chapchap.auth.domain.user.controller;

import com.chapchap.auth.domain.user.request.UserRoleChangeRequest;
import com.chapchap.auth.domain.user.response.UserRoleChangeResponse;
import com.chapchap.auth.domain.user.service.UserRoleService;
import com.chapchap.auth.global.response.GlobalResponse;
import com.chapchap.auth.global.error.custom.business.InvalidTokenException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "라이더 역할 API", description = "CUSTOMER와 RIDER 역할 전환")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/admin/users")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class UserRoleController {
    private final UserRoleService userRoleService;

    @Operation(summary = "일반 사용자 역할 변경", description = "CUSTOMER와 RIDER 사이에서만 변경하며 기존 세션을 폐기합니다.")
    @PatchMapping("/{userId}/role")
    public ResponseEntity<GlobalResponse<UserRoleChangeResponse>> changeRole(
            Authentication authentication,
            @PathVariable Long userId,
            @Valid @RequestBody UserRoleChangeRequest request
    ) {
        return GlobalResponse.success(userRoleService.changeGeneralUserRole(
                currentUserId(authentication), userId, request.targetRole()
        ));
    }

    private Long currentUserId(Authentication authentication) {
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new InvalidTokenException("유효하지 않은 사용자 식별자입니다.");
        }
    }
}
