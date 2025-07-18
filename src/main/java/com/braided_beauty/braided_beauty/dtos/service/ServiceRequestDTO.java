package com.braided_beauty.braided_beauty.dtos.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@AllArgsConstructor
@Getter
@Setter
public class ServiceRequestDTO {
    private final String name;
    private final String Description;
    private final BigDecimal price;
    private final Integer durationMinutes;
}
