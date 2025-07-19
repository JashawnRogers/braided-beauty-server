package com.braided_beauty.braided_beauty.dtos.service;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@AllArgsConstructor
@Getter
@Setter
public class ServiceRequestDTO {
    @NotNull
    private final String name;
    @NotNull
    private final String Description;
    @NotNull
    private final BigDecimal price;
    @NotNull
    private final Integer durationMinutes;
    private String photoUrl;
    private String videoUrl;
}
