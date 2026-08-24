package com.chapchap.auth.domain.policy.response;

import com.chapchap.auth.global.security.constant.ConsentStatusPolicy;

import java.time.LocalDateTime;

public record MarketingConsentResponse(
        Long policyId,
        ConsentStatusPolicy consentStatus,
        LocalDateTime decidedAt
) {
}
