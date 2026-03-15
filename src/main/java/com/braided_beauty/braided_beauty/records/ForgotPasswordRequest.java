package com.braided_beauty.braided_beauty.records;

import jakarta.validation.constraints.Email;

public record ForgotPasswordRequest(@Email String email) {
}
