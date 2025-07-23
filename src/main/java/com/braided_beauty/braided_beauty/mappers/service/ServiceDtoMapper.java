package com.braided_beauty.braided_beauty.mappers.service;

import com.braided_beauty.braided_beauty.dtos.service.ServiceRequestDTO;
import com.braided_beauty.braided_beauty.dtos.service.ServiceResponseDTO;
import com.braided_beauty.braided_beauty.models.ServiceModel;
import org.springframework.stereotype.Component;

@Component
public class ServiceDtoMapper {

    public ServiceModel toEntity(ServiceRequestDTO dto){
        return ServiceModel.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .depositAmount(dto.getDepositAmount())
                .durationMinutes(dto.getDurationMinutes())
                .photoUrl(dto.getPhotoUrl())
                .videoUrl(dto.getVideoUrl())
                .build();
    }

    public ServiceResponseDTO toDTO(ServiceModel serviceModel){
        return ServiceResponseDTO.builder()
                .id(serviceModel.getId())
                .name(serviceModel.getName())
                .description(serviceModel.getDescription())
                .price(serviceModel.getPrice())
                .depositAmount(serviceModel.getDepositAmount())
                .durationMinutes(serviceModel.getDurationMinutes())
                .photoUrl(serviceModel.getPhotoUrl())
                .videoUrl(serviceModel.getVideoUrl())
                .build();
    }
}
