package com.chapchap.auth.domain.audit.controller;

import com.chapchap.auth.domain.audit.response.AuditLogResponse;
import com.chapchap.auth.domain.audit.service.AuditLogService;
import com.chapchap.auth.global.response.GlobalResponse;
import com.chapchap.auth.global.error.custom.business.InvalidParameterException;
import com.chapchap.auth.domain.user.constant.RolePolicy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "감사 로그 API", description = "관리자 보안·권한 변경 이력 조회")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/audit-logs")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AuditLogController {
    private final AuditLogService auditLogService;

    @Operation(summary = "감사 로그 조회", description = "SUPER_ADMIN은 전체, ADMIN은 라이더 역할 변경 이력만 조회합니다.")
    @GetMapping
    public ResponseEntity<GlobalResponse<Page<AuditLogResponse>>> search(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidParameterException("페이지 조건이 올바르지 않습니다.");
        }
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return GlobalResponse.success(auditLogService.search(requesterRole(authentication), pageable));
    }

    private RolePolicy requesterRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> RolePolicy.valueOf(authority.substring("ROLE_".length())))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("감사 로그를 조회할 권한이 없습니다."));
    }
}
