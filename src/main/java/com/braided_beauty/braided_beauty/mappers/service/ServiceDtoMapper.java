package com.braided_beauty.braided_beauty.mappers.service;

import com.braided_beauty.braided_beauty.dtos.service.PopularServiceDTO;
import com.braided_beauty.braided_beauty.dtos.service.ServiceCreateDTO;
import com.braided_beauty.braided_beauty.dtos.service.ServiceRequestDTO;
import com.braided_beauty.braided_beauty.dtos.service.ServiceResponseDTO;
import com.braided_beauty.braided_beauty.models.ServiceModel;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.math.RoundingMode;
import java.util.*;

import static com.braided_beauty.braided_beauty.utils.Deposit.getDepositAmount;

@Component
public class ServiceDtoMapper {

    private static List<String> normalizeKeys(@Nullable List<String> src) {
        if (src == null || src.isEmpty()) return List.of();
        return src.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }


    public ServiceModel create(ServiceCreateDTO dto){
        ServiceModel service = new ServiceModel();

        if (dto.getName() != null && !dto.getName().isBlank()) service.setName(dto.getName());
        if (dto.getDescription() != null) service.setDescription(dto.getDescription());
        if (dto.getCategory() != null && !dto.getCategory().getName().isBlank()) service.setCategory(dto.getCategory());
        if (dto.getPrice() != null) service.setPrice(dto.getPrice().setScale(2, RoundingMode.UNNECESSARY));
        if (dto.getPrice() != null) service.setDepositAmount(getDepositAmount(dto.getPrice()));
        if (dto.getDurationMinutes() != null) service.setDurationMinutes(dto.getDurationMinutes());

        List<String> photos = normalizeKeys(dto.getPhotoKeys());
        if (!photos.isEmpty()) service.setPhotoKeys(photos);
        if (dto.getVideoKey() != null && !dto.getVideoKey().isBlank()) {
            service.setVideoKey(dto.getVideoKey().trim());
        }
        return service;
    }

    public ServiceResponseDTO toDto(ServiceModel service){
        return ServiceResponseDTO.builder()
                .id(service.getId())
                .category(service.getCategory())
                .name(service.getName())
                .description(service.getDescription())
                .price(service.getPrice())
                .depositAmount(service.getDepositAmount())
                .durationMinutes(service.getDurationMinutes())
                .photoKeys(Optional.ofNullable(service.getPhotoKeys()).orElse(List.of()))
                .videoKey(service.getVideoKey())
                .build();
    }

    public void updateDto(ServiceRequestDTO dto, ServiceModel service) {
        if (dto.getName() != null && !dto.getName().isBlank()) service.setName(dto.getName().trim());
        if (dto.getCategory() != null) service.setCategory(dto.getCategory());
        if (dto.getDescription() != null) service.setDescription(dto.getDescription().trim());
        if (dto.getPrice() != null) service.setPrice(dto.getPrice().setScale(2, RoundingMode.UNNECESSARY));
        if (dto.getDepositAmount() != null) service.setDepositAmount(dto.getDepositAmount());
        if (dto.getDurationMinutes() != null) service.setDurationMinutes(dto.getDurationMinutes());

        // Photos: incremental add/remove
        LinkedHashSet<String> keys = new LinkedHashSet<>(Optional.ofNullable(service.getPhotoKeys()).orElse(List.of()));

        List<String> toRemove = normalizeKeys(dto.getRemovePhotoKeys());
        List<String> toAdd = normalizeKeys(dto.getAddPhotoKeys());

        if (!toRemove.isEmpty() || !toAdd.isEmpty()) {
            toRemove.forEach(keys::remove);
            keys.addAll(toAdd);
            service.setPhotoKeys(new ArrayList<>(keys));
        }

        /*
         * Video:
         * - null -> keep
         * - "" -> clear
         * - non-null -> set/update
         */
        if (dto.getVideoKey() != null){
            String videoKey = dto.getVideoKey().trim();
            service.setVideoKey(videoKey.isBlank() ? null : videoKey);
        }
    }

    public PopularServiceDTO toMostPopularServiceDto(ServiceModel service){
        int booked = Optional.ofNullable(service.getTimesBooked()).orElse(0);
        return PopularServiceDTO.builder()
                .serviceName(service.getName())
                .completedCount(booked)
                .build();
    }
}
