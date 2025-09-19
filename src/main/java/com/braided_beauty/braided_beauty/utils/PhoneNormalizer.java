package com.braided_beauty.braided_beauty.utils;

import jakarta.annotation.Nullable;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class PhoneNormalizer {
// Normalize to E.164 (+1XXXXXXXXXX) for US-only; returns null if blank/invalid
    @Nullable
    public static String toE164(@Nullable String raw) {
        if (raw == null) return null;

        // Trim & normalize whitespace
        String s = raw.trim();
        if (s.isEmpty()) return null;

        // Keep leading '+' if present, strip everything else non-digit
        boolean hasPlus = s.startsWith("+");
        String digits = s.replaceAll("\\D", "");

        // If user typed 10 US digits, assume +1
        if (digits.length() == 10) return "+1" + digits;

        // If they typed +1XXXXXXXXXX (11 digits) or 1XXXXXXXXXX and we had '+'
        if (hasPlus && digits.length() == 11 && digits.startsWith("1")) {
            return "+" + digits;
        }

        // Otherwise reject as invalid
        return null;
    }
}
