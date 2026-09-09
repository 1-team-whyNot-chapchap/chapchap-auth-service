package com.chapchap.auth.domain.user.service;

import com.chapchap.auth.domain.user.repository.UserRepository;
import com.chapchap.auth.domain.user.response.AdminUserResponse;
import com.chapchap.auth.global.error.custom.business.InvalidTokenException;
import com.chapchap.auth.global.security.constant.UserStatusPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class CurrentUserService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public AdminUserResponse getCurrentUser(Long userId, Set<String> authorities) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("사용할 수 없는 계정입니다."));
        if (user.getStatus() != UserStatusPolicy.ACTIVE || !authorities.contains("ROLE_" + user.getRole().name())) {
            throw new InvalidTokenException("계정 상태가 변경되었습니다. 다시 로그인해 주세요.");
        }
        return new AdminUserResponse(user.getId().toString(), user.getName(), user.getRole(), user.getStatus());
    }
}
