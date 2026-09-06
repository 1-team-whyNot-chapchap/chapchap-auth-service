package com.chapchap.auth.domain.admin.controller;

import com.chapchap.auth.domain.admin.request.AdminAccountCreateRequest;
import com.chapchap.auth.domain.admin.request.AdminPasswordChangeRequest;
import com.chapchap.auth.domain.admin.request.AdminPasswordResetRequest;
import com.chapchap.auth.domain.admin.request.InitialAdminPasswordChangeRequest;
import com.chapchap.auth.domain.admin.response.AdminAccountResponse;
import com.chapchap.auth.domain.admin.service.AdminAccountService;
import com.chapchap.auth.global.cookie.CookieManager;
import com.chapchap.auth.global.response.GlobalResponse;
import com.chapchap.auth.global.error.custom.business.InvalidTokenException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 계정 API", description = "비밀번호와 관리자 계정 운영")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/admin")
public class AdminAccountController {
    private final AdminAccountService adminAccountService;
    private final CookieManager cookieManager;

    @Operation(summary = "최초 또는 재설정 비밀번호 변경")
    @PostMapping("/password/initial")
    public ResponseEntity<GlobalResponse<Void>> changeInitialPassword(
            @Valid @RequestBody InitialAdminPasswordChangeRequest request
    ) {
        adminAccountService.changeInitialPassword(request);
        return GlobalResponse.success();
    }

    @Operation(summary = "내 관리자 비밀번호 변경")
    @PostMapping("/password")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<GlobalResponse<Void>> changeOwnPassword(
            Authentication authentication,
            @Valid @RequestBody AdminPasswordChangeRequest request,
            HttpServletResponse response
    ) {
        adminAccountService.changeOwnPassword(currentUserId(authentication), request);
        cookieManager.removeRefreshTokenToCookie(response);
        return GlobalResponse.success();
    }

    @Operation(summary = "ADMIN 계정 발급")
    @PostMapping("/accounts")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<GlobalResponse<AdminAccountResponse>> createAdmin(
            Authentication authentication,
            @Valid @RequestBody AdminAccountCreateRequest request
    ) {
        return GlobalResponse.success(adminAccountService.createAdmin(currentUserId(authentication), request));
    }

    @Operation(summary = "다른 관리자 임시 비밀번호 재설정")
    @PostMapping("/accounts/{userId}/password-reset")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<GlobalResponse<Void>> resetPassword(
            Authentication authentication,
            @PathVariable Long userId,
            @Valid @RequestBody AdminPasswordResetRequest request
    ) {
        adminAccountService.resetPassword(currentUserId(authentication), userId, request);
        return GlobalResponse.success();
    }

    @Operation(summary = "다른 ADMIN 계정 비활성화")
    @PostMapping("/accounts/{userId}/disable")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<GlobalResponse<Void>> disableAdmin(Authentication authentication, @PathVariable Long userId) {
        adminAccountService.disableAdmin(currentUserId(authentication), userId);
        return GlobalResponse.success();
    }

    @Operation(summary = "다른 관리자 잠금 해제")
    @PostMapping("/accounts/{userId}/unlock")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<GlobalResponse<Void>> unlockAdmin(Authentication authentication, @PathVariable Long userId) {
        adminAccountService.unlockAdmin(currentUserId(authentication), userId);
        return GlobalResponse.success();
    }

    private Long currentUserId(Authentication authentication) {
        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new InvalidTokenException("유효하지 않은 사용자 식별자입니다.");
        }
    }
}
