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

    /**
     * Formats an E.164 number for email display.
     * Examples:
     *  +16021234567 -> (602) 123-4567
     *  16021234567  -> (602) 123-4567
     *  6021234567   -> (602) 123-4567
     *
     * If it's not a US 10-digit number, it returns the original sanitized value.
     */
    public static String formatForEmail(String e164OrMessy) {
        if (e164OrMessy == null || e164OrMessy.isBlank()) return "";

        // keep digits only
        String digits = e164OrMessy.replaceAll("\\D", "");

        // handle +1XXXXXXXXXX or 1XXXXXXXXXX
        if (digits.length() == 11 && digits.startsWith("1")) {
            digits = digits.substring(1);
        }

        // if we have 10 digits, format as (xxx) xxx-xxxx
        if (digits.length() == 10) {
            String area = digits.substring(0, 3);
            String prefix = digits.substring(3, 6);
            String line = digits.substring(6);
            return "(" + area + ") " + prefix + "-" + line;
        }

        // fallback: return something readable
        return e164OrMessy.trim();
    }
}
