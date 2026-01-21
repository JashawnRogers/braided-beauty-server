package com.braided_beauty.braided_beauty.records;

public record OAuthIdentity(
        String provider,
        String subject,
        String name,
        String email
) {
}
