package com.chapchap.auth.domain.policy.response;

import com.chapchap.auth.domain.policy.entity.Policy;
import com.chapchap.auth.global.security.constant.PolicyTypePolicy;

public record CurrentPolicyResponse(
        Long policyId,
        PolicyTypePolicy policyType,
        String version,
        String title,
        String content,
        boolean required
) {
    public static CurrentPolicyResponse from(Policy policy) {
        return new CurrentPolicyResponse(
                policy.getId(),
                policy.getPolicyType(),
                policy.getVersion(),
                policy.getTitle(),
                policy.getContent(),
                policy.isRequired()
        );
    }
}
