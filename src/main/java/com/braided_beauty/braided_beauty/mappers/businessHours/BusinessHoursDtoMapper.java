package com.braided_beauty.braided_beauty.mappers.businessHours;

import com.braided_beauty.braided_beauty.dtos.shared.BusinessHoursRequestDTO;
import com.braided_beauty.braided_beauty.dtos.shared.BusinessHoursResponseDTO;
import com.braided_beauty.braided_beauty.models.BusinessHours;
import org.springframework.stereotype.Component;

@Component
public class BusinessHoursDtoMapper {

    public BusinessHours toEntity(BusinessHoursRequestDTO dto){
        BusinessHours entity = new BusinessHours();
        entity.setDayOfWeek(dto.getDayOfWeek());
        entity.setOpenTime(dto.getOpenTime());
        entity.setCloseTime(dto.getCloseTime());
        entity.setClosed(dto.isClosed());
        return entity;
    }

    public void update(BusinessHours target, BusinessHoursRequestDTO dto) {
        target.setDayOfWeek(dto.getDayOfWeek());
        target.setOpenTime(dto.getOpenTime());
        target.setCloseTime(dto.getCloseTime());
        target.setClosed(dto.isClosed());
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
