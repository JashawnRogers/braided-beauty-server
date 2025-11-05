package com.braided_beauty.braided_beauty.dtos.serviceCategory;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class ServiceCategoryUpdateDTO {
    @Nullable
    private final UUID id;
    @NotBlank(message = "Must provide a name for the category")
    private final String name;
}
