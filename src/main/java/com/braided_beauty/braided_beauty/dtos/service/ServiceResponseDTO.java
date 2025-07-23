package com.braided_beauty.braided_beauty.dtos.service;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@AllArgsConstructor
@Getter
@Setter
public class ServiceResponseDTO {
    private final UUID id;
    private final String name;
    private final String description;
    private final BigDecimal price;
    private final BigDecimal depositAmount;
    private final Integer durationMinutes;
    private final Integer pointsEarned;
    private String photoUrl;
    private String videoUrl;
}
