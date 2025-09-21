package com.braided_beauty.braided_beauty.utils;

import lombok.NoArgsConstructor;

import java.util.Optional;

@NoArgsConstructor
public final class PhoneNormalizer {
    /**
     *   Normalize to E.164 for US numbers; empty/blank -> empty Optional.
     *   Handles the empty string sometimes returned from React Admin.
     * */
    public static Optional<String> toE164(String input) {
        if (input == null) return Optional.empty();
        String digits = input.replaceAll("\\D+", "");
        if (digits.isBlank()) return Optional.empty();

        // Accept 10 digits (assume US) or 11 starting with '1'
        if (digits.length() == 10) return Optional.of("+1" + digits);
        if (digits.length() == 11 && digits.startsWith("1")) return Optional.of("+" + digits);

        throw new IllegalArgumentException("Invalid phone number");
    }
}
