package com.braided_beauty.braided_beauty.dtos.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@AllArgsConstructor
@Getter
@Setter
public class ServiceRequestDTO {
    @NotBlank(message = "Service name is required.")
    private final String name;
    @NotBlank(message = "Service must have a description.")
    private final String Description;
    @PositiveOrZero(message = "Price must be zero or more.")
    private final BigDecimal price;
    @PositiveOrZero(message = "Deposit must be zero or more.")
    private final BigDecimal depositAmount;
    @Positive(message = "Duration of service must be greater than zero.")
    private final Integer durationMinutes;
    private String photoUrl;
    private String videoUrl;
}
