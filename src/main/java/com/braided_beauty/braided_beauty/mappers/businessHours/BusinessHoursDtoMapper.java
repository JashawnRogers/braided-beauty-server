package com.braided_beauty.braided_beauty.mappers.businessHours;

import com.braided_beauty.braided_beauty.dtos.shared.BusinessHoursRequestDTO;
import com.braided_beauty.braided_beauty.dtos.shared.BusinessHoursResponseDTO;
import com.braided_beauty.braided_beauty.models.BusinessHours;
import org.springframework.stereotype.Component;

@Component
public class BusinessHoursDtoMapper {

    public BusinessHours toEntity(BusinessHoursRequestDTO dto){
        return BusinessHours.builder()
                .dayOfWeek(dto.getDayOfWeek())
                .openTime(dto.getOpenTime())
                .closeTime(dto.getCloseTime())
                .isClosed(dto.isClosed())
                .build();
    }

    public BusinessHoursResponseDTO toDTO(BusinessHours dto){
        return BusinessHoursResponseDTO.builder()
                .id(dto.getId())
                .dayOfWeek(dto.getDayOfWeek())
                .openTime(dto.getOpenTime())
                .closeTime(dto.getCloseTime())
                .isClosed(dto.isClosed())
                .build();
    }
}
