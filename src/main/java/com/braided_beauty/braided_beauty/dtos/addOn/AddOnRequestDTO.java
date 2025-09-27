package com.braided_beauty.braided_beauty.dtos.addOn;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AddOnRequestDTO {
    private UUID id;
    @NotBlank(message = "must provide a name for the add on.")
    private String name;
    @PositiveOrZero(message = "price must be zero or more.")
    private BigDecimal price;
    private String description;
}
