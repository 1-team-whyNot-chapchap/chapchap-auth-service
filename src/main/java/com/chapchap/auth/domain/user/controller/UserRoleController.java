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

    @Operation(summary = "고객의 라이더 승격 또는 고객 역할 복귀", description = "ADMIN 또는 SUPER_ADMIN 전용입니다. "
            + "ACTIVE CUSTOMER를 라이더로 승격하려면 targetRole=RIDER를 전송합니다. "
            + "RIDER를 고객으로 복귀시키려면 targetRole=CUSTOMER를 전송합니다. "
            + "관리자 대상, 비활성 사용자, 이미 같은 역할인 요청은 거절합니다. "
            + "성공 시 기존 로그인 세션을 폐기하고 감사 이력과 역할 변경 이벤트를 기록합니다. "
            + "대상 사용자는 재로그인하여 변경된 역할의 토큰을 발급받아야 합니다.")
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
