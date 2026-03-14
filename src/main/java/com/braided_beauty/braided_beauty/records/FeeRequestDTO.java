package com.braided_beauty.braided_beauty.records;

import jakarta.validation.constraints.Min;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;
import java.util.UUID;

public record FeeRequestDTO(
        UUID id,
        @Length(max = 100)
        String name,
        @Min(0)
        BigDecimal amount
) {
}
