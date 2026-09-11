package com.chapchap.auth.domain.user.controller;

import com.chapchap.auth.domain.policy.request.MarketingConsentRequest;
import com.chapchap.auth.domain.policy.response.MarketingConsentResponse;
import com.chapchap.auth.domain.policy.service.MarketingConsentService;
import com.chapchap.auth.domain.user.response.UserProfileResponse;
import com.chapchap.auth.domain.user.request.WithdrawalRequest;
import com.chapchap.auth.domain.user.service.UserProfileService;
import com.chapchap.auth.global.response.GlobalResponse;
import com.chapchap.auth.global.error.custom.business.InvalidTokenException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "내 계정 API", description = "일반 사용자 프로필과 선택 동의 관리")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/users/me")
@PreAuthorize("hasAnyRole('CUSTOMER', 'RIDER')")
public class UserProfileController {
    private final UserProfileService userProfileService;
    private final MarketingConsentService marketingConsentService;

    @Operation(summary = "내 프로필 조회")
    @GetMapping
    public ResponseEntity<GlobalResponse<UserProfileResponse>> getMyProfile(Authentication authentication) {
        return GlobalResponse.success(userProfileService.getMyProfile(currentUserId(authentication)));
    }

    @Operation(summary = "프로필 이미지 등록 또는 교체", description = "JPEG, PNG, WebP만 허용하며 최대 5MB입니다.")
    @PostMapping(value = "/profile-image", consumes = "multipart/form-data")
    public ResponseEntity<GlobalResponse<UserProfileResponse>> replaceProfileImage(
            Authentication authentication,
            @RequestPart("file") MultipartFile file
    ) {
        return GlobalResponse.success(userProfileService.replaceProfileImage(currentUserId(authentication), file));
    }

    @Operation(summary = "프로필 이미지 삭제")
    @DeleteMapping("/profile-image")
    public ResponseEntity<GlobalResponse<Void>> deleteProfileImage(Authentication authentication) {
        userProfileService.deleteProfileImage(currentUserId(authentication));
        return GlobalResponse.success();
    }

    @Operation(summary = "이메일 마케팅 동의 변경")
    @PostMapping("/marketing-consent")
    public ResponseEntity<GlobalResponse<MarketingConsentResponse>> changeMarketingConsent(
            Authentication authentication,
            @Valid @RequestBody MarketingConsentRequest request
    ) {
        return GlobalResponse.success(marketingConsentService.change(currentUserId(authentication), request));
    }

    @Operation(summary = "회원 탈퇴", description = "confirmed=true이고 Auth DB의 구독 Projection이 INACTIVE인 경우에만 처리합니다.")
    @DeleteMapping
    public ResponseEntity<GlobalResponse<Void>> withdraw(
            Authentication authentication,
            @Valid @RequestBody WithdrawalRequest request
    ) {
        userProfileService.withdraw(currentUserId(authentication));
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
