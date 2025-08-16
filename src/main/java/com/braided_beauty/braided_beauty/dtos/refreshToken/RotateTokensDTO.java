package com.braided_beauty.braided_beauty.dtos.refreshToken;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
public class RotateTokensDTO {
    @NotNull
    private final String newAccessToken;
    @NotNull
    private final String newRefreshToken;
    @NotNull
    private final Instant refreshExpiresAt;
}
