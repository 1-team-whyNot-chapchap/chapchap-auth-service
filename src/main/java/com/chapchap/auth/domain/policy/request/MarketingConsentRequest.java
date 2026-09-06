package com.chapchap.auth.domain.policy.request;

import jakarta.validation.constraints.NotNull;

public record MarketingConsentRequest(
        @NotNull Long policyId,
        @NotNull Boolean agreed
) {
}
