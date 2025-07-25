package com.braided_beauty.braided_beauty.dtos.service;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@AllArgsConstructor
@Getter
@Setter
public class ServicePatchDTO {
    @Size(min = 1, max = 150, message = "Name must be at least 1 character and more than 150.")
    private String name;
    @Max(value = 1000, message = "Description must be no more than 1000 characters.")
    private String description;
    @Min(value = -1, message = "Price must not be a negative number.")
    private BigDecimal price;
    @Min(value = -1, message = "Deposit amount must not be a negative number.")
    private BigDecimal depositAmount;
    @Min(value = -1, message = "Duration of service must not be a negative number.")
    private Integer durationMinutes;
    private String photoUrl;
    private String videoUrl;
}
