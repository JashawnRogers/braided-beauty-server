package com.braided_beauty.braided_beauty.records;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record FeeResponseDTO(
        UUID id,
        String name,
        BigDecimal amount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean active
) {
}
