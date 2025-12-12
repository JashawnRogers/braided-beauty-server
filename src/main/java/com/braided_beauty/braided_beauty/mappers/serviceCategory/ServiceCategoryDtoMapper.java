package com.braided_beauty.braided_beauty.mappers.serviceCategory;

import com.braided_beauty.braided_beauty.dtos.serviceCategory.ServiceCategoryCreateDTO;
import com.braided_beauty.braided_beauty.dtos.serviceCategory.ServiceCategoryUpdateDTO;
import com.braided_beauty.braided_beauty.dtos.serviceCategory.ServiceCategoryResponseDTO;
import com.braided_beauty.braided_beauty.models.ServiceCategory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ServiceCategoryDtoMapper {
    public ServiceCategory create(ServiceCategoryCreateDTO dto) {
        return ServiceCategory.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .build();
    }

    public ServiceCategory update(ServiceCategory target, ServiceCategoryUpdateDTO dto) {
        target.setName(dto.getName());
        target.setDescription(dto.getDescription());
        return target;
    }

    public ServiceCategoryResponseDTO toDto(ServiceCategory entity) {
        return ServiceCategoryResponseDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .build();
    }
}
