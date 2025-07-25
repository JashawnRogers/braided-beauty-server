package com.braided_beauty.braided_beauty.mappers.service;

import com.braided_beauty.braided_beauty.dtos.service.ServicePatchDTO;
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

    public void updateServiceFromPatchDTO(ServicePatchDTO dto, ServiceModel service) {
        if (dto.getName() != null) service.setName(dto.getName());
        if (dto.getDescription() != null) service.setDescription(dto.getDescription());
        if (dto.getPrice() != null) service.setPrice(dto.getPrice());
        if (dto.getDepositAmount() != null) service.setDepositAmount(dto.getDepositAmount());
        if (dto.getDurationMinutes() != null) service.setDurationMinutes(dto.getDurationMinutes());
        if (dto.getPhotoUrl() != null) service.setPhotoUrl(dto.getPhotoUrl());
        if (dto.getVideoUrl() != null) service.setVideoUrl(dto.getVideoUrl());
    }
}
