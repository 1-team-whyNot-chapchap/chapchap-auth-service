package com.chapchap.auth.domain.auth.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PolicyConsentRequest(
        @NotNull
        @Positive
        Long policyId,

        @NotNull
        Boolean agreed
) {
}
