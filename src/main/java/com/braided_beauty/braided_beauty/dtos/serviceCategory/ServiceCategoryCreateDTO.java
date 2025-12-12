package com.braided_beauty.braided_beauty.dtos.serviceCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ServiceCategoryCreateDTO {
    @NotBlank(message = "Name is required")
    private String name;
    @Size(max = 250, message = "Description must be less than 250 characters")
    private String description;
}
