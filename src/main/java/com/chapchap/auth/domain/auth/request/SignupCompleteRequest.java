package com.chapchap.auth.domain.auth.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SignupCompleteRequest(

        @NotBlank
        String signupSessionId,

        @NotBlank
        String identityVerificationId,

        @Valid
        @NotEmpty
        List<PolicyConsentRequest> policies

){
}
