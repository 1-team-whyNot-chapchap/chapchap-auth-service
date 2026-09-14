package com.chapchap.auth.domain.admin.response;

import com.chapchap.auth.domain.admin.entity.AdminCredential;
import java.time.LocalDateTime;

public record AdminAccountSummary(Long userId, String name, String username, String role,
        String status, boolean mustChangePassword, boolean locked, LocalDateTime lockedUntil,
        LocalDateTime lastLoginAt, LocalDateTime createdAt) {
    public static AdminAccountSummary from(AdminCredential c, LocalDateTime now) {
        return new AdminAccountSummary(c.getUser().getId(), c.getUser().getName(), c.getUsername(),
                c.getUser().getRole().name(), c.getUser().getStatus().name(), c.isMustChangePassword(),
                c.isLockedAt(now), c.getLockedUntil(), c.getLastLoginAt(), c.getCreatedAt());
    }
}
