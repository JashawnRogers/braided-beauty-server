package com.braided_beauty.braided_beauty.dtos.serviceCategory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
@Builder
public class ServiceCategoryResponseDTO {
    private UUID id;
    private String name;
}
