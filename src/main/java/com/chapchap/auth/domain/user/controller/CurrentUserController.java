package com.chapchap.auth.domain.user.controller;

import com.chapchap.auth.domain.user.service.CurrentUserService;
import com.chapchap.auth.domain.user.response.AdminUserResponse;
import com.chapchap.auth.global.error.custom.business.InvalidTokenException;
import com.chapchap.auth.global.response.GlobalResponse;
import com.chapchap.auth.global.response.constant.CustomResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "현재 인증 사용자 API")
public class CurrentUserController {
    private final CurrentUserService currentUserService;

    @GetMapping("/api/auth/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'RIDER', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "현재 인증된 본인의 역할과 상태 확인", description = "본인의 최소 정보만 반환합니다. "
            + "비활성 또는 토큰과 현재 역할이 다르면 401/E05로 재로그인이 필요합니다.")
    public ResponseEntity<GlobalResponse<AdminUserResponse>> getCurrentUser(Authentication authentication) {
        Long userId;
        try {
            userId = Long.valueOf(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new InvalidTokenException("유효하지 않은 사용자 식별자입니다.");
        }
        var user = currentUserService.getCurrentUser(userId, authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toSet()));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(GlobalResponse.from(
                CustomResponseCode.SUCCESS,
                user));
    }
}
