package com.braided_beauty.braided_beauty.dtos.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@AllArgsConstructor
@Getter
@Setter
public class ServicePatchDTO {
    private String name;
    private String description;
    private BigDecimal price; // Use boxed types (Integer, not int)
    private BigDecimal depositAmount;
    private Integer durationMinutes;
    private String photoUrl;
    private String videoUrl;
}
