package com.braided_beauty.braided_beauty.dtos.service;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class ServiceCreateDTO {
    @Size(min = 1, max = 150)
    private String name;
    @Size(max = 500)
    private String description;
    @DecimalMin(value = "0.00", message = "Price amount must not be a negative number.")
    private BigDecimal price;
    @DecimalMin(value = "0.00", message = "Deposit amount must not be a negative number.")
    private BigDecimal depositAmount;
    @Min(value = -1, message = "Duration of service must not be a negative number")
    private Integer durationMinutes;
    private List<String> photoKeys;
    private String videoKey;
}
