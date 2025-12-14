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

    public BusinessHoursResponseDTO toDTO(BusinessHours entity){
        return BusinessHoursResponseDTO.builder()
                .id(entity.getId())
                .dayOfWeek(entity.getDayOfWeek())
                .openTime(entity.getOpenTime())
                .closeTime(entity.getCloseTime())
                .isClosed(entity.isClosed())
                .build();
    }
}
