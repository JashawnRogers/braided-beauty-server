package com.braided_beauty.braided_beauty.dtos.serviceCategory;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ServiceCategoryCreateDTO {
    @NotBlank(message = "Name is required")
    private final String name;
}
