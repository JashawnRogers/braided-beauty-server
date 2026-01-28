package com.braided_beauty.braided_beauty.records;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailRequest(
        @Email @NotBlank String to,
        @NotBlank String subject,
        @NotBlank String body,
        @NotBlank String html
) {
}
