package com.braided_beauty.braided_beauty.records;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank String newPassword,
        @NotBlank String confirmPassword
) {
}
