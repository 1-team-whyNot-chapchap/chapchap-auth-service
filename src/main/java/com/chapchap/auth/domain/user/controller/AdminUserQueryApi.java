package com.chapchap.auth.domain.user.controller;

import com.chapchap.auth.domain.user.request.AdminUserSearchRequest;
import com.chapchap.auth.domain.user.response.AdminUserResponse;
import com.chapchap.auth.domain.user.response.AdminUserSearchResponse;
import com.chapchap.auth.global.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자 사용자 조회 API", description = "ADMIN/SUPER_ADMIN 전용. 응답 캐시 금지")
@ApiResponse(responseCode = "400", description = "E21 잘못된 입력")
@ApiResponse(responseCode = "401", description = "E03 인증 필요")
@ApiResponse(responseCode = "403", description = "E04 관리자 권한 필요")
public interface AdminUserQueryApi {
    @Operation(summary = "전체 휴대폰 번호 정확 일치 검색", description = "번호의 공백/하이픈을 제거해 비교합니다. "
            + "번호를 URL에 남기지 않도록 POST body를 사용합니다. ID 오름차순 20명씩 반환하며 중복 번호를 모두 포함합니다. "
            + "자동 선택하지 말고 사용자 ID와 이름을 대조해야 합니다. 0건도 200으로 반환합니다.")
    @PostMapping("/api/auth/admin/users/search")
    ResponseEntity<GlobalResponse<AdminUserSearchResponse>> search(@Valid @RequestBody AdminUserSearchRequest request);

    @Operation(summary = "승격 전 사용자 최신 상태 조회", description = "양의 정수 사용자 ID로 조회합니다. 승격 가능 여부는 PATCH 시 다시 검사합니다.")
    @ApiResponse(responseCode = "404", description = "E10 사용자 없음")
    @GetMapping("/api/auth/admin/users/{userId}")
    ResponseEntity<GlobalResponse<AdminUserResponse>> getUser(@PathVariable Long userId);
}
