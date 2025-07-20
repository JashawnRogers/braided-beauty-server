package com.braided_beauty.braided_beauty.mappers.service;

import com.braided_beauty.braided_beauty.dtos.service.ServiceRequestDTO;
import com.braided_beauty.braided_beauty.dtos.service.ServiceResponseDTO;
import com.braided_beauty.braided_beauty.models.Service;
import org.springframework.stereotype.Component;

@Component
public class ServiceDtoMapper {

    public Service toEntity(ServiceRequestDTO dto){
        return Service.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .durationMinutes(dto.getDurationMinutes())
                .photoUrl(dto.getPhotoUrl())
                .videoUrl(dto.getVideoUrl())
                .build();
    }

    public ServiceResponseDTO toDTO(Service service){
        return ServiceResponseDTO.builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .price(service.getPrice())
                .durationMinutes(service.getDurationMinutes())
                .photoUrl(service.getPhotoUrl())
                .videoUrl(service.getVideoUrl())
                .build();
    }
}
