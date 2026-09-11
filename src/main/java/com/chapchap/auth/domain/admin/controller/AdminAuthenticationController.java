package com.chapchap.auth.domain.admin.controller;

import com.chapchap.auth.domain.admin.request.AdminLoginRequest;
import com.chapchap.auth.domain.admin.response.AdminLoginResponse;
import com.chapchap.auth.domain.admin.dto.AdminLoginResult;
import com.chapchap.auth.domain.admin.service.AdminAuthenticationService;
import com.chapchap.auth.global.service.cookie.CookieManager;
import com.chapchap.auth.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 인증 API", description = "관리자 로그인과 잠금 정책")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/admin")
public class AdminAuthenticationController {
    private final AdminAuthenticationService adminAuthenticationService;
    private final CookieManager cookieManager;

    @Operation(summary = "관리자 로그인", description = "실패 원인은 외부에 구분하지 않으며 잠금 상태도 동일 응답으로 처리합니다.")
    @PostMapping("/login")
    public ResponseEntity<GlobalResponse<AdminLoginResponse>> login(
            @Valid @RequestBody AdminLoginRequest request,
            HttpServletResponse response
    ) {
        AdminLoginResult loginResult = adminAuthenticationService.login(request);
        if (loginResult.issuedToken() != null) {
            cookieManager.setRefreshTokenToCookie(
                    response,
                    loginResult.issuedToken().refreshToken(),
                    loginResult.issuedToken().sessionType()
            );
        } else {
            cookieManager.removeRefreshTokenToCookie(response);
        }
        return GlobalResponse.success(loginResult.response());
    }
}
