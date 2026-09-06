package com.chapchap.auth.domain.auth.controller;

import com.chapchap.auth.domain.auth.dto.IssuedToken;
import com.chapchap.auth.domain.auth.dto.ReissuedToken;
import com.chapchap.auth.domain.auth.request.SignupCompleteRequest;
import com.chapchap.auth.domain.auth.response.AccessTokenResponse;
import com.chapchap.auth.domain.auth.service.AuthService;
import com.chapchap.auth.domain.auth.service.SignupService;
import com.chapchap.auth.global.cookie.CookieManager;
import com.chapchap.auth.global.error.custom.business.InvalidTokenException;
import com.chapchap.auth.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증 API", description = "인증 담당")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final SignupService signupService;
    private final CookieManager cookieManager;
    private final AuthService authService;

    // 본인인증과 정책 동의를 검증하고 회원가입 또는 계정 연결을 완료
    @PostMapping("/signup/complete")
    public ResponseEntity<GlobalResponse<AccessTokenResponse>> completeSignup(
        @Valid @RequestBody SignupCompleteRequest request,
        HttpServletResponse response
        ) {
        
        // 가입 완료 후 Access/Refresh Token 발급
        IssuedToken issuedToken = signupService.completeSignup(request);

        // Refresh Token은 HttpOnly Cookie에만 저장
        cookieManager.setRefreshTokenToCookie(
            response,
            issuedToken.refreshToken(),
            issuedToken.sessionType()
        );

        // Access Token만 응답 Body로 전달

        return GlobalResponse.success(
            new AccessTokenResponse(
                issuedToken.accessToken()
            )
        );
    }

    // Refresh Token을 검증하고 새로운 Access/Refresh Token을 발급
    @PostMapping("/reissue-token")
    public ResponseEntity<GlobalResponse<AccessTokenResponse>> reissueToken(
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        try {
            // HttpOnly Cookie에서 Refresh Token 원문 조회
            String refreshToken = cookieManager.getRefreshTokenToCookie(request)
                                      .orElseThrow(() ->
                                                      new InvalidTokenException(
                                                          "리프레시 토큰이 존재하지 않습니다."
                                                      )
                                                  );

            // 기존 Refresh Token을 소비하고 새로운 Token Pair 발급
            ReissuedToken reissuedToken = authService.reissueRefreshToken(refreshToken);

            // 새 Refresh Token은 다시 HttpOnly Cookie에 저장
            cookieManager.setRefreshTokenToCookie(
                response,
                reissuedToken.refreshToken(),
                reissuedToken.sessionType()
            );

            // 새 Access Token만 Response Body로 반환
            return GlobalResponse.success(
                new AccessTokenResponse(
                    reissuedToken.accessToken()
                )
            );
        } catch (InvalidTokenException e) {

            // 사용할 수 없는 Refresh Token Cookie 제거
            cookieManager.removeRefreshTokenToCookie(response);

            throw e;
        }
    }

    // 현재 로그인 세션을 종료하고 Refresh Token 쿠키 삭제
    @PostMapping("/logout")
    public ResponseEntity<GlobalResponse<Void>> logout(
        HttpServletRequest request,
        HttpServletResponse response
    ) {

        // Refresh Token이 존재하면 연결된 서버 세션 폐기
        cookieManager.getRefreshTokenToCookie(request)
            .ifPresent(authService::logout);

        // 쿠키 존재 여부와 관계없이 브라우저 Refresh Token 제거
        cookieManager.removeRefreshTokenToCookie(response);

        return GlobalResponse.success();
    }
}
