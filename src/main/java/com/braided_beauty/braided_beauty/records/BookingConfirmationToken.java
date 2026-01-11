package com.braided_beauty.braided_beauty.records;

import java.time.Instant;

public record BookingConfirmationToken(String token, String jti, Instant expiresAt) {
}
