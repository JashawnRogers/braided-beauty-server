package com.braided_beauty.braided_beauty.dtos.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class ServiceResponseDTO {
    private final UUID id;
    private final String name;
    private final String description;
    private final BigDecimal price;
    private final Integer durationMinutes;
    private String photoUrl;
    private String videoUrl;
}
