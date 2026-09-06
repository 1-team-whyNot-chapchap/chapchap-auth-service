package com.chapchap.auth.domain.user.response;

import com.chapchap.auth.global.security.constant.ProviderPolicy;
import com.chapchap.auth.global.security.constant.RolePolicy;
import com.chapchap.auth.global.security.constant.UserStatusPolicy;

import java.util.List;

public record UserProfileResponse(
        String name,
        String phone,
        String email,
        RolePolicy role,
        UserStatusPolicy status,
        Long defaultAddressId,
        List<ProviderPolicy> connectedProviders,
        String profileImageUrl
) {
}
