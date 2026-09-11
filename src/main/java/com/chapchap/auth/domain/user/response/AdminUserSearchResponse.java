package com.chapchap.auth.domain.user.response;

import java.util.List;

public record AdminUserSearchResponse(
        List<AdminUserResponse> users,
        int page,
        long totalElements,
        boolean hasNext
) {
    public AdminUserSearchResponse {
        users = List.copyOf(users);
    }
}
