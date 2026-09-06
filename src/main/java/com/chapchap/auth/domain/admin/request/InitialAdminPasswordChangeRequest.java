package com.chapchap.auth.domain.admin.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InitialAdminPasswordChangeRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(max = 64) String currentPassword,
        @NotBlank @Size(max = 64) String newPassword,
        @NotBlank @Size(max = 64) String newPasswordConfirmation
) {
}
