package com.chapchap.auth.domain.user.service;

import com.chapchap.auth.domain.user.repository.UserRepository;
import com.chapchap.auth.global.security.constant.RolePolicy;
import com.chapchap.auth.global.security.constant.UserStatusPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("activeAdministratorAccess")
@RequiredArgsConstructor
public class ActiveAdministratorAccess {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public boolean isAllowed(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        final long userId;
        try {
            userId = Long.parseLong(authentication.getName());
        } catch (NumberFormatException exception) {
            return false;
        }
        if (userId <= 0) return false;
        return userRepository.findById(userId).filter(user ->
                user.getStatus() == UserStatusPolicy.ACTIVE
                && (user.getRole() == RolePolicy.ADMIN || user.getRole() == RolePolicy.SUPER_ADMIN)
                && authentication.getAuthorities().stream().anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_" + user.getRole().name()))).isPresent();
    }
}
