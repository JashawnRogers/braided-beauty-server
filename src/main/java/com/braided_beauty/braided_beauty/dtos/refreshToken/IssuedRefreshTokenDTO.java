package com.braided_beauty.braided_beauty.dtos.refreshToken;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
public class IssuedRefreshTokenDTO {
    @NotNull
    private final String refreshToken;
    @Nullable
    private final Instant expiresAt;
}
