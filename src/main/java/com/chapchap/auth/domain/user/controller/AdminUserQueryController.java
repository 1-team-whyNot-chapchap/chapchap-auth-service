package com.chapchap.auth.domain.user.controller;

import com.chapchap.auth.domain.user.request.AdminUserSearchRequest;
import com.chapchap.auth.domain.user.response.AdminUserResponse;
import com.chapchap.auth.domain.user.response.AdminUserSearchResponse;
import com.chapchap.auth.domain.user.service.AdminUserQueryService;
import com.chapchap.auth.global.response.GlobalResponse;
import com.chapchap.auth.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') and @activeAdministratorAccess.isAllowed(authentication)")
public class AdminUserQueryController implements AdminUserQueryApi {
    private final AdminUserQueryService adminUserQueryService;

    @Override
    public ResponseEntity<GlobalResponse<AdminUserSearchResponse>> search(AdminUserSearchRequest request) {
        return noStore(adminUserQueryService.search(request));
    }

    @Override
    public ResponseEntity<GlobalResponse<AdminUserResponse>> getUser(Long userId) {
        return noStore(adminUserQueryService.getUser(userId));
    }

    private <T> ResponseEntity<GlobalResponse<T>> noStore(T data) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(GlobalResponse.from(CustomResponseCode.SUCCESS, data));
    }
}
