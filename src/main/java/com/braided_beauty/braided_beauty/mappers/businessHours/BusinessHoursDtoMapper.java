package com.braided_beauty.braided_beauty.mappers.businessHours;

import com.braided_beauty.braided_beauty.dtos.shared.BusinessHoursRequestDTO;
import com.braided_beauty.braided_beauty.dtos.shared.BusinessHoursResponseDTO;
import com.braided_beauty.braided_beauty.models.BusinessHours;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;

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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");

        String open = dto.getOpenTime() != null ? dto.getOpenTime().format(formatter) : "-";
        String close = dto.getCloseTime() != null ? dto.getCloseTime().format(formatter) : "-";
        String isClosed = dto.isClosed() ? "Closed" : "Open";
        String dayOfWeek = dto.getDayOfWeek() != null ? switch (dto.getDayOfWeek()) {
            case DayOfWeek.SUNDAY -> "Sunday";
            case DayOfWeek.MONDAY -> "Monday";
            case DayOfWeek.TUESDAY -> "Tuesday";
            case DayOfWeek.WEDNESDAY -> "Wednesday";
            case DayOfWeek.THURSDAY -> "Thursday";
            case DayOfWeek.FRIDAY -> "Friday";
            case DayOfWeek.SATURDAY -> "Saturday";
        } : "-";

        return BusinessHoursResponseDTO.builder()
                .id(dto.getId())
                .dayOfWeek(dayOfWeek)
                .openTime(open)
                .closeTime(close)
                .isClosed(isClosed)
                .build();
    }
}
