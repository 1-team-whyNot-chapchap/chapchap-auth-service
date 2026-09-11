package com.chapchap.auth.domain.user.service;

import com.chapchap.auth.domain.user.entity.User;
import com.chapchap.auth.domain.user.repository.UserRepository;
import com.chapchap.auth.domain.user.request.AdminUserSearchRequest;
import com.chapchap.auth.domain.user.response.AdminUserResponse;
import com.chapchap.auth.domain.user.response.AdminUserSearchResponse;
import com.chapchap.auth.global.error.custom.business.InvalidParameterException;
import com.chapchap.auth.global.error.custom.business.NotFoundResourceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserQueryService {
    private final UserRepository userRepository;

    public AdminUserSearchResponse search(AdminUserSearchRequest request) {
        String phone = normalizePhone(request.phone());
        if (request.page() < 0 || request.page() > 1000000) {
            throw new InvalidParameterException("유효하지 않은 검색 페이지입니다.");
        }
        var users = userRepository.findByExactPhone(phone,
                PageRequest.of(request.page(), 20, Sort.by("id").ascending()));
        return new AdminUserSearchResponse(users.map(this::response).getContent(),
                users.getNumber(), users.getTotalElements(), users.hasNext());
    }

    public AdminUserResponse getUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new InvalidParameterException("유효하지 않은 사용자 ID입니다.");
        }
        return response(userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundResourceException("사용자를 찾을 수 없습니다.")));
    }

    private String normalizePhone(String value) {
        if (value == null || value.length() > 30 || !value.matches("[0-9 -]+")) {
            throw new InvalidParameterException("전체 휴대폰 번호를 입력해 주세요.");
        }
        String phone = value.replace("-", "").replace(" ", "");
        if (!phone.matches("(?:010[0-9]{8}|01[16789][0-9]{7,8})")) {
            throw new InvalidParameterException("전체 휴대폰 번호를 입력해 주세요.");
        }
        return phone;
    }

    private AdminUserResponse response(User user) {
        return new AdminUserResponse(user.getId().toString(), user.getName(), user.getRole(), user.getStatus());
    }
}
