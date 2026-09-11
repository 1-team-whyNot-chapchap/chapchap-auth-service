package com.chapchap.auth.domain.admin.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminLoginRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(max = 64) String password
) {
}
