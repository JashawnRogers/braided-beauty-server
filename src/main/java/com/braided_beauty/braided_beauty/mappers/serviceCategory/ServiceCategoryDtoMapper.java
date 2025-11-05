package com.braided_beauty.braided_beauty.mappers.serviceCategory;

import com.braided_beauty.braided_beauty.dtos.serviceCategory.ServiceCategoryUpdateDTO;
import com.braided_beauty.braided_beauty.dtos.serviceCategory.ServiceCategoryResponseDTO;
import com.braided_beauty.braided_beauty.models.ServiceCategory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ServiceCategoryDtoMapper {
    public ServiceCategory create(String name) {
        return ServiceCategory.builder()
                .name(name)
                .build();
    }

    public ServiceCategory update(ServiceCategory target, String name) {
        target.setName(name);
        return target;
    }

    public ServiceCategoryResponseDTO toDto(ServiceCategory entity) {
        return ServiceCategoryResponseDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }
}
