package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.shared.BusinessHoursRequestDTO;
import com.braided_beauty.braided_beauty.dtos.shared.BusinessHoursResponseDTO;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.models.ScheduleCalendar;
import com.braided_beauty.braided_beauty.models.ScheduleCalendarHours;
import com.braided_beauty.braided_beauty.repositories.ScheduleCalendarHoursRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessHoursService {
    private final ScheduleCalendarHoursRepository hoursRepository;
    private final ScheduleCalendarService scheduleCalendarService;

    @Transactional
    public BusinessHoursResponseDTO create(BusinessHoursRequestDTO dto){
        validate(dto);
        ScheduleCalendar calendar = scheduleCalendarService.getDefaultCalendar();

        if (hoursRepository.existsByCalendar_IdAndDayOfWeek(calendar.getId(), dto.getDayOfWeek())) {
            throw new IllegalArgumentException("Hours for " + dto.getDayOfWeek() + " already exists.");
        }

        ScheduleCalendarHours entity = toEntity(dto, calendar);
        hoursRepository.save(entity);
        log.info("Created default-calendar hours entity. ID: {}", entity.getId());
        return toDto(entity);
    }

    @Transactional
    public BusinessHoursResponseDTO update(UUID id, BusinessHoursRequestDTO dto) {
        validate(dto);

        ScheduleCalendarHours entity = hoursRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Business hours not found: " + id));

        UUID calendarId = entity.getCalendar().getId();
        if (!entity.getDayOfWeek().equals(dto.getDayOfWeek())
                && hoursRepository.existsByCalendar_IdAndDayOfWeekAndIdNot(calendarId, dto.getDayOfWeek(), id)) {
            throw new IllegalArgumentException("Hours for " + dto.getDayOfWeek() + " already exist.");
        }

        entity.setDayOfWeek(dto.getDayOfWeek());
        entity.setOpenTime(dto.getOpenTime());
        entity.setCloseTime(dto.getCloseTime());
        entity.setClosed(dto.isClosed());

        if (entity.isClosed()) {
            entity.setOpenTime(null);
            entity.setCloseTime(null);
        }

        entity = hoursRepository.save(entity);
        log.info("Updated hours entity, ID: {}", entity.getId());
        return toDto(entity);
    }

    @Transactional
    public void delete(UUID id) {
        if (!hoursRepository.existsById(id)) {
            throw new NotFoundException("Business hours not found: " + id);
        }
        log.info("Deleted hours entity ID: {}", id);
        hoursRepository.deleteById(id);
    }

    public BusinessHoursResponseDTO getOne(UUID id) {
        ScheduleCalendarHours entity = hoursRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Business hours not found: " + id));
        return toDto(entity);
    }

    public List<BusinessHoursResponseDTO> getAll() {
        ScheduleCalendar calendar = scheduleCalendarService.getDefaultCalendar();
        return hoursRepository.findAllByCalendar_IdOrderByDayOfWeekAsc(calendar.getId())
                .stream()
                .map(BusinessHoursService::toDto)
                .toList();
    }

    private static ScheduleCalendarHours toEntity(BusinessHoursRequestDTO dto, ScheduleCalendar calendar) {
        return ScheduleCalendarHours.builder()
                .calendar(calendar)
                .dayOfWeek(dto.getDayOfWeek())
                .openTime(dto.getOpenTime())
                .closeTime(dto.getCloseTime())
                .isClosed(dto.isClosed())
                .build();
    }

    private static BusinessHoursResponseDTO toDto(ScheduleCalendarHours entity) {
        return BusinessHoursResponseDTO.builder()
                .id(entity.getId())
                .dayOfWeek(entity.getDayOfWeek())
                .openTime(entity.getOpenTime())
                .closeTime(entity.getCloseTime())
                .isClosed(entity.isClosed())
                .build();
    }

    private void validate(BusinessHoursRequestDTO dto) {
        if (dto.isClosed()) {
            if (dto.getOpenTime() != null || dto.getCloseTime() != null) {
                throw new IllegalArgumentException("Closed days must not have open/close times");
            }
            return;
        }

        if (dto.getOpenTime() == null || dto.getCloseTime() == null) {
            throw new IllegalArgumentException("Open days must include both openTime and closeTime");
        }

        if (!dto.getOpenTime().isBefore(dto.getCloseTime())) {
            throw new IllegalArgumentException("openTime must be before closeTime");
        }
    }
}
