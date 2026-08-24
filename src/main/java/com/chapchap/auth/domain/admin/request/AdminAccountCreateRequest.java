package com.chapchap.auth.domain.admin.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminAccountCreateRequest(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(max = 64) String temporaryPassword
) {
}
