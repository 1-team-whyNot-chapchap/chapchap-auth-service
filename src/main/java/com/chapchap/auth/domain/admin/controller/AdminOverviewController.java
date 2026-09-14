package com.chapchap.auth.domain.admin.controller;

import com.chapchap.auth.domain.admin.service.AdminOverviewService;
import com.chapchap.auth.domain.admin.response.AdminAccountSummary;
import com.chapchap.auth.global.response.GlobalResponse;
import com.chapchap.auth.global.error.custom.business.InvalidParameterException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/admin")
public class AdminOverviewController {
    private final AdminOverviewService overview;

    @GetMapping("/accounts")
    @PreAuthorize("hasRole('SUPER_ADMIN') and @activeAdministratorAccess.isAllowed(authentication)")
    public ResponseEntity<GlobalResponse<AccountPage>> accounts(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        if (search.length() > 50 || page < 0 || size < 1 || size > 100) throw new InvalidParameterException("조회 조건을 확인해 주세요.");
        Page<AdminAccountSummary> result = overview.accounts(search, page, size);
        return noStore(new AccountPage(result.getContent(), page, size, result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("overview")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN') and @activeAdministratorAccess.isAllowed(authentication)")
    public ResponseEntity<GlobalResponse<AdminOverviewService.Overview>> stats(@RequestParam(defaultValue = "30") int days) {
        if (days != 7 && days != 30 && days != 90) throw new InvalidParameterException("조회 기간은 7일, 30일, 90일 중 선택해 주세요.");
        return noStore(overview.overview(days));
    }

    private <T> ResponseEntity<GlobalResponse<T>> noStore(T data) {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(GlobalResponse.from(com.chapchap.auth.global.response.constant.CustomResponseCode.SUCCESS, data));
    }

    public record AccountPage(java.util.List<AdminAccountSummary> content, int page, int size, long totalElements, int totalPages) {}
}
