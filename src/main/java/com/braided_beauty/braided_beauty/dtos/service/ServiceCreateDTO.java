package com.braided_beauty.braided_beauty.dtos.service;

import com.braided_beauty.braided_beauty.models.ServiceCategory;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@Getter
@Setter
public class ServiceCreateDTO {
    @NotNull
    private UUID categoryId;
    @Size(min = 1, max = 150)
    private String name;
    @Size(max = 500)
    private String description;
    @DecimalMin(value = "0.00", message = "Price amount must not be a negative number.")
    private BigDecimal price;
    @Min(value = -1, message = "Duration of service must not be a negative number")
    private Integer durationMinutes;
    private List<String> photoKeys;
    private String videoKey;
    @Nullable
    private List<UUID> addOnIds = List.of();
}
