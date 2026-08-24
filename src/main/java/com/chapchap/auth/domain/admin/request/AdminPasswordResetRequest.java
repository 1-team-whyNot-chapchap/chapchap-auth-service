package com.chapchap.auth.domain.admin.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminPasswordResetRequest(
        @NotBlank @Size(max = 64) String temporaryPassword
) {
}
