package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.calendar.*;
import com.braided_beauty.braided_beauty.exceptions.ConflictException;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.models.ScheduleCalendar;
import com.braided_beauty.braided_beauty.models.ScheduleCalendarDateOverride;
import com.braided_beauty.braided_beauty.models.ScheduleCalendarHours;
import com.braided_beauty.braided_beauty.models.ServiceModel;
import com.braided_beauty.braided_beauty.repositories.AppointmentRepository;
import com.braided_beauty.braided_beauty.repositories.ScheduleCalendarDateOverrideRepository;
import com.braided_beauty.braided_beauty.repositories.ScheduleCalendarHoursRepository;
import com.braided_beauty.braided_beauty.repositories.ScheduleCalendarRepository;
import com.braided_beauty.braided_beauty.repositories.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleCalendarService {
    private final ScheduleCalendarRepository scheduleCalendarRepository;
    private final ScheduleCalendarHoursRepository scheduleCalendarHoursRepository;
    private final ScheduleCalendarDateOverrideRepository scheduleCalendarDateOverrideRepository;
    private final AppointmentRepository appointmentRepository;
    private final ServiceRepository serviceRepository;

    public ScheduleCalendar getDefaultCalendar() {
        return scheduleCalendarRepository.findByNameIgnoreCase("Default")
                .orElseThrow(() -> new NotFoundException("Default schedule calendar is not configured."));
    }

    public ScheduleCalendar resolveByIdOrDefault(UUID calendarId) {
        if (calendarId == null) {
            return getDefaultCalendar();
        }

        return scheduleCalendarRepository.findById(calendarId)
                .orElseThrow(() -> new NotFoundException("Schedule calendar not found with ID: " + calendarId));
    }

    public List<ScheduleCalendar> getAdminCalendars() {
        return scheduleCalendarRepository.findAllByOrderByNameAsc();
    }

    @Transactional
    public void deleteCalendar(UUID calendarId) {
        ScheduleCalendar calendar = scheduleCalendarRepository.findByIdForUpdate(calendarId)
                .orElseThrow(() -> new NotFoundException("Schedule calendar not found with ID: " + calendarId));

        if ("default".equalsIgnoreCase(calendar.getName())) {
            throw new ConflictException("Default calendar cannot be deleted.");
        }

        if (appointmentRepository.existsByScheduleCalendar_Id(calendarId)) {
            throw new ConflictException("Cannot delete calendar with existing appointments.");
        }

        ScheduleCalendar defaultCalendar = getDefaultCalendar();
        List<ServiceModel> servicesOnCalendar = serviceRepository.findAllByScheduleCalendar_Id(calendarId);
        if (!servicesOnCalendar.isEmpty()) {
            servicesOnCalendar.forEach(service -> service.setScheduleCalendar(defaultCalendar));
            serviceRepository.saveAll(servicesOnCalendar);
        }

        scheduleCalendarHoursRepository.deleteAllByCalendar_Id(calendarId);
        scheduleCalendarDateOverrideRepository.deleteAllByCalendar_Id(calendarId);
        scheduleCalendarRepository.delete(calendar);
    }

    public List<AdminCalendarHoursDTO> getWeeklyHours(UUID calendarId) {
        ScheduleCalendar calendar = resolveByIdOrDefault(calendarId);
        Map<DayOfWeek, ScheduleCalendarHours> existingByDay =
                scheduleCalendarHoursRepository.findAllByCalendar_IdOrderByDayOfWeekAsc(calendar.getId())
                        .stream()
                        .collect(Collectors.toMap(ScheduleCalendarHours::getDayOfWeek, Function.identity()));

        return Arrays.stream(DayOfWeek.values())
                .map(day -> toHoursDto(day, existingByDay.get(day)))
                .toList();
    }

    public AdminCalendarDTO toAdminCalendarDto(ScheduleCalendar calendar) {
        return AdminCalendarDTO.builder()
                .id(calendar.getId())
                .name(calendar.getName())
                .color(calendar.getColor())
                .active(calendar.isActive())
                .maxBookingsPerDay(calendar.getMaxBookingsPerDay())
                .bookingOpenAt(calendar.getBookingOpenAt())
                .bookingCloseAt(calendar.getBookingCloseAt())
                .build();
    }

    @Transactional
    public AdminCalendarDTO createCalendar(AdminCalendarCreateRequestDTO dto) {
        ScheduleCalendar calendar = ScheduleCalendar.builder()
                .name(dto.getName().trim())
                .color(normalizeColor(dto.getColor()))
                .active(dto.getActive() == null || dto.getActive())
                .maxBookingsPerDay(sanitizeMaxBookings(dto.getMaxBookingsPerDay()))
                .bookingOpenAt(dto.getBookingOpenAt())
                .bookingCloseAt(dto.getBookingCloseAt())
                .build();

        validateBookingWindow(calendar.getBookingOpenAt(), calendar.getBookingCloseAt());
        ScheduleCalendar saved = scheduleCalendarRepository.save(calendar);
        return toAdminCalendarDto(saved);
    }

    @Transactional
    public AdminCalendarDTO updateCalendar(UUID id, AdminCalendarUpdateRequestDTO dto) {
        ScheduleCalendar calendar = scheduleCalendarRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Schedule calendar not found with ID: " + id));

        if (dto.getName() != null && !dto.getName().isBlank()) {
            calendar.setName(dto.getName().trim());
        }
        if (dto.getColor() != null) {
            calendar.setColor(normalizeColor(dto.getColor()));
        }
        if (dto.getActive() != null) {
            calendar.setActive(dto.getActive());
        }
        if (dto.getMaxBookingsPerDay() != null) {
            calendar.setMaxBookingsPerDay(sanitizeMaxBookings(dto.getMaxBookingsPerDay()));
        }
        if (dto.getBookingOpenAt() != null) {
            calendar.setBookingOpenAt(dto.getBookingOpenAt());
        }
        if (dto.getBookingCloseAt() != null) {
            calendar.setBookingCloseAt(dto.getBookingCloseAt());
        }

        validateBookingWindow(calendar.getBookingOpenAt(), calendar.getBookingCloseAt());
        return toAdminCalendarDto(scheduleCalendarRepository.save(calendar));
    }

    @Transactional
    public void upsertWeeklyHours(UUID calendarId, List<AdminCalendarHoursUpsertDTO> hours) {
        ScheduleCalendar calendar = resolveByIdOrDefault(calendarId);
        validateWeeklyHours(hours);

        Map<DayOfWeek, ScheduleCalendarHours> existingByDay =
                scheduleCalendarHoursRepository.findAllByCalendar_IdOrderByDayOfWeekAsc(calendar.getId())
                        .stream()
                        .collect(Collectors.toMap(ScheduleCalendarHours::getDayOfWeek, Function.identity()));

        for (AdminCalendarHoursUpsertDTO row : hours) {
            validateOpenClose(row.getIsClosed(), row.getOpenTime(), row.getCloseTime());
            ScheduleCalendarHours entity = existingByDay.get(row.getDayOfWeek());
            if (entity == null) {
                entity = ScheduleCalendarHours.builder()
                        .calendar(calendar)
                        .dayOfWeek(row.getDayOfWeek())
                        .build();
            }
            entity.setClosed(row.getIsClosed());
            entity.setOpenTime(row.getIsClosed() ? null : row.getOpenTime());
            entity.setCloseTime(row.getIsClosed() ? null : row.getCloseTime());
            scheduleCalendarHoursRepository.save(entity);
        }
    }

    public List<AdminCalendarOverrideDTO> getOverrides(UUID calendarId, LocalDate start, LocalDate end) {
        resolveByIdOrDefault(calendarId);
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("start must be on or before end");
        }
        return scheduleCalendarDateOverrideRepository
                .findAllByCalendar_IdAndDateBetweenOrderByDateAsc(calendarId, start, end)
                .stream()
                .map(this::toOverrideDto)
                .toList();
    }

    @Transactional
    public List<AdminCalendarOverrideDTO> upsertOverrides(UUID calendarId, List<AdminCalendarOverrideUpsertDTO> rows) {
        ScheduleCalendar calendar = resolveByIdOrDefault(calendarId);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        Set<LocalDate> uniqueDates = rows.stream().map(AdminCalendarOverrideUpsertDTO::getDate).collect(Collectors.toSet());
        if (rows.size() != uniqueDates.size()) {
            throw new IllegalArgumentException("Override dates must be unique.");
        }

        for (AdminCalendarOverrideUpsertDTO row : rows) {
            validateOpenClose(row.getIsClosed(), row.getOpenTime(), row.getCloseTime());
            ScheduleCalendarDateOverride entity = scheduleCalendarDateOverrideRepository
                    .findByCalendar_IdAndDate(calendarId, row.getDate())
                    .orElseGet(() -> ScheduleCalendarDateOverride.builder()
                            .calendar(calendar)
                            .date(row.getDate())
                            .build());

            entity.setClosed(row.getIsClosed());
            entity.setOpenTime(row.getIsClosed() ? null : row.getOpenTime());
            entity.setCloseTime(row.getIsClosed() ? null : row.getCloseTime());
            scheduleCalendarDateOverrideRepository.save(entity);
        }

        return uniqueDates.stream()
                .map(date -> scheduleCalendarDateOverrideRepository.findByCalendar_IdAndDate(calendarId, date)
                        .orElseThrow(() -> new NotFoundException("Override not found for date: " + date)))
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .map(this::toOverrideDto)
                .toList();
    }

    @Transactional
    public void deleteOverride(UUID calendarId, UUID overrideId) {
        resolveByIdOrDefault(calendarId);
        ScheduleCalendarDateOverride override = scheduleCalendarDateOverrideRepository.findById(overrideId)
                .orElseThrow(() -> new NotFoundException("Override not found: " + overrideId));
        if (!override.getCalendar().getId().equals(calendarId)) {
            throw new NotFoundException("Override not found for calendar.");
        }
        scheduleCalendarDateOverrideRepository.delete(override);
    }

    public ScheduleCalendarDateOverride getOverride(UUID calendarId, LocalDate date) {
        return scheduleCalendarDateOverrideRepository.findByCalendar_IdAndDate(calendarId, date).orElse(null);
    }

    public EffectiveCalendarConstraints getEffectiveCalendarConstraints(UUID calendarId, LocalDate date) {
        ScheduleCalendar calendar = resolveByIdOrDefault(calendarId);
        return getEffectiveCalendarConstraints(calendar, date);
    }

    public EffectiveCalendarConstraints getEffectiveCalendarConstraints(ScheduleCalendar calendar, LocalDate date) {
        ScheduleCalendarDateOverride override = getOverride(calendar.getId(), date);
        if (override != null) {
            return toConstraints(date, override.isClosed(), override.getOpenTime(), override.getCloseTime());
        }

        ScheduleCalendarHours weeklyHours = scheduleCalendarHoursRepository
                .findByCalendar_IdAndDayOfWeek(calendar.getId(), date.getDayOfWeek())
                .orElse(null);

        if (weeklyHours == null) {
            return closedConstraints();
        }

        return toConstraints(date, weeklyHours.isClosed(), weeklyHours.getOpenTime(), weeklyHours.getCloseTime());
    }

    public boolean isWithinBookingWindow(ScheduleCalendar calendar, LocalDateTime at) {
        if (calendar.getBookingOpenAt() != null && at.isBefore(calendar.getBookingOpenAt())) {
            return false;
        }
        return calendar.getBookingCloseAt() == null || !at.isAfter(calendar.getBookingCloseAt());
    }

    public boolean isDateWithinBookingWindow(ScheduleCalendar calendar, LocalDate date) {
        LocalDateTime dateStart = date.atStartOfDay();
        LocalDateTime dateEnd = date.plusDays(1).atStartOfDay();
        LocalDateTime openAt = calendar.getBookingOpenAt();
        LocalDateTime closeAt = calendar.getBookingCloseAt();

        boolean startsBeforeOrAtClose = closeAt == null || !dateStart.isAfter(closeAt);
        boolean endsAfterOrAtOpen = openAt == null || !dateEnd.isBefore(openAt);
        return startsBeforeOrAtClose && endsAfterOrAtOpen;
    }

    private static void validateWeeklyHours(List<AdminCalendarHoursUpsertDTO> hours) {
        if (hours == null || hours.size() != 7) {
            throw new IllegalArgumentException("Exactly 7 day rows are required.");
        }
        Set<DayOfWeek> days = hours.stream().map(AdminCalendarHoursUpsertDTO::getDayOfWeek).collect(Collectors.toSet());
        if (days.size() != 7 || !days.containsAll(Arrays.asList(DayOfWeek.values()))) {
            throw new IllegalArgumentException("Hours payload must contain each day exactly once.");
        }
    }

    private static void validateOpenClose(Boolean isClosed, java.time.LocalTime openTime, java.time.LocalTime closeTime) {
        if (isClosed == null) {
            throw new IllegalArgumentException("isClosed is required.");
        }
        if (isClosed) {
            if (openTime != null || closeTime != null) {
                throw new IllegalArgumentException("Closed rows must not include openTime/closeTime.");
            }
            return;
        }
        if (openTime == null || closeTime == null) {
            throw new IllegalArgumentException("Open rows must include both openTime and closeTime.");
        }
        if (!openTime.isBefore(closeTime)) {
            throw new IllegalArgumentException("openTime must be before closeTime.");
        }
    }

    private static int sanitizeMaxBookings(Integer maxBookingsPerDay) {
        int value = maxBookingsPerDay == null ? 0 : maxBookingsPerDay;
        if (value < 0) {
            throw new IllegalArgumentException("maxBookingsPerDay cannot be negative.");
        }
        return value;
    }

    private static String normalizeColor(String color) {
        if (color == null || color.isBlank()) {
            return "#1D4ED8";
        }
        return color.trim();
    }

    private static void validateBookingWindow(LocalDateTime openAt, LocalDateTime closeAt) {
        if (openAt != null && closeAt != null && closeAt.isBefore(openAt)) {
            throw new IllegalArgumentException("bookingCloseAt must be on or after bookingOpenAt.");
        }
    }

    private AdminCalendarOverrideDTO toOverrideDto(ScheduleCalendarDateOverride entity) {
        return AdminCalendarOverrideDTO.builder()
                .id(entity.getId())
                .date(entity.getDate())
                .isClosed(entity.isClosed())
                .openTime(entity.getOpenTime())
                .closeTime(entity.getCloseTime())
                .build();
    }

    private static AdminCalendarHoursDTO toHoursDto(DayOfWeek dayOfWeek, ScheduleCalendarHours entity) {
        if (entity == null) {
            return AdminCalendarHoursDTO.builder()
                    .dayOfWeek(dayOfWeek)
                    .isClosed(true)
                    .openTime(null)
                    .closeTime(null)
                    .build();
        }

        return AdminCalendarHoursDTO.builder()
                .dayOfWeek(dayOfWeek)
                .isClosed(entity.isClosed())
                .openTime(entity.getOpenTime())
                .closeTime(entity.getCloseTime())
                .build();
    }

    private static EffectiveCalendarConstraints toConstraints(
            LocalDate date,
            boolean isClosed,
            LocalTime openTime,
            LocalTime closeTime
    ) {
        if (isClosed || openTime == null || closeTime == null || !openTime.isBefore(closeTime)) {
            return closedConstraints();
        }

        return new EffectiveCalendarConstraints(
                false,
                openTime,
                closeTime,
                LocalDateTime.of(date, openTime),
                LocalDateTime.of(date, closeTime)
        );
    }

    private static EffectiveCalendarConstraints closedConstraints() {
        return new EffectiveCalendarConstraints(true, null, null, null, null);
    }

    public record EffectiveCalendarConstraints(
            boolean isClosed,
            LocalTime effectiveOpenTime,
            LocalTime effectiveCloseTime,
            LocalDateTime openAt,
            LocalDateTime closeAt
    ) {}
}
