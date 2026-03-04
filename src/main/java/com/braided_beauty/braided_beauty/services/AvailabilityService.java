package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.timeSlots.AvailableTimeSlotsDTO;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.models.*;
import com.braided_beauty.braided_beauty.repositories.AppointmentRepository;
import com.braided_beauty.braided_beauty.repositories.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AvailabilityService {
    private final ServiceRepository serviceRepository;
    private final AppointmentRepository appointmentRepository;
    private final BusinessSettingsService businessSettingsService;
    private final AddOnService addOnService;
    private final ScheduleCalendarService scheduleCalendarService;


    public List<AvailableTimeSlotsDTO> getAvailability(UUID serviceId, LocalDate date, List<UUID> addOnIds) {
        ServiceModel service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new NotFoundException("Service not found with ID: " + serviceId));

        ScheduleCalendar calendar = service.getScheduleCalendar();
        if (calendar == null) {
            throw new NotFoundException("Service is missing a schedule calendar");
        }

        if (!scheduleCalendarService.isDateWithinBookingWindow(calendar, date)) {
            return List.of();
        }

        ScheduleCalendarService.EffectiveCalendarConstraints constraints =
                scheduleCalendarService.getEffectiveCalendarConstraints(calendar, date);
        if (constraints.isClosed()) {
            return List.of();
        }

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
        if (isDailyCapReached(calendar, dayStart, dayEnd)) {
            return List.of();
        }

        int slotMinutes = 30;
        int bufferMinutes = businessSettingsService.getAppointmentBufferMinutes();

        int serviceDuration = service.getDurationMinutes() == null ? 0 : service.getDurationMinutes();
        int addOnMinutes = 0;

        if (addOnIds != null && !addOnIds.isEmpty()) {
            addOnMinutes = addOnService.getAddOnIds(addOnIds).stream()
                    .map(AddOn::getDurationMinutes)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();
        }

        int requestedDuration = serviceDuration + addOnMinutes;

        LocalDateTime open = constraints.openAt();
        LocalDateTime close = constraints.closeAt();
        if (open == null || close == null) {
            return List.of();
        }

        if (calendar.getBookingOpenAt() != null && calendar.getBookingOpenAt().isAfter(open)) {
            open = calendar.getBookingOpenAt();
        }
        if (calendar.getBookingCloseAt() != null && calendar.getBookingCloseAt().isBefore(close)) {
            close = calendar.getBookingCloseAt();
        }
        if (!open.isBefore(close)) {
            return List.of();
        }

        if (date.equals(LocalDate.now())) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime minStart = ceilToSlot(now, slotMinutes);
            if (minStart.isAfter(open)) {
                open = minStart;
            }
        }

        // If single stylist: block against ALL appointments, not by serviceId.
        List<Appointment> appointments =
                appointmentRepository.findBlockingAppointmentsForWindow(open, close, calendar.getId());

        List<TimeRange> blocked = appointments.stream()
                .map(a -> {
                    LocalDateTime start = a.getAppointmentTime();
                    int apptDuration = a.getDurationMinutes();

                    LocalDateTime blockedUntil = start.plusMinutes(apptDuration + bufferMinutes);
                    LocalDateTime blockedUntilAligned = ceilToSlot(blockedUntil, slotMinutes);

                    return new TimeRange(start, blockedUntilAligned);
                })
                .toList();

        LocalDateTime latestStart = close.minusMinutes(requestedDuration + bufferMinutes);
        if (open.isAfter(latestStart)) {
            return List.of();
        }

        List<AvailableTimeSlotsDTO> out = new ArrayList<>();
        for (LocalDateTime slotStart = open; !slotStart.isAfter(latestStart); slotStart = slotStart.plusMinutes(slotMinutes)) {
            LocalDateTime slotEnd = slotStart.plusMinutes(requestedDuration + bufferMinutes);

            LocalDateTime finalSlotStart = slotStart;
            boolean blockedAny = blocked.stream()
                    .anyMatch(b -> isOverlap(finalSlotStart, slotEnd, b.start, b.end));

            out.add(new AvailableTimeSlotsDTO(
                    slotStart.toLocalTime().toString(),
                    !blockedAny,
                    slotStart,
                    slotEnd
            ));
        }

        return out;
    }

    private boolean isDailyCapReached(ScheduleCalendar calendar, LocalDateTime dayStart, LocalDateTime dayEnd) {
        int maxBookingsPerDay = calendar.getMaxBookingsPerDay();
        if (maxBookingsPerDay <= 0) {
            return false;
        }

        long currentBookings = appointmentRepository.countBlockingBookingsForDay(dayStart, dayEnd, calendar.getId());
        return currentBookings >= maxBookingsPerDay;
    }

    private boolean isOverlap(LocalDateTime aStart, LocalDateTime aEnd, LocalDateTime bStart, LocalDateTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    private LocalDateTime ceilToSlot(LocalDateTime t, int slotMinutes) {
        int minute = t.getMinute();
        int mod = minute % slotMinutes;

        // already aligned (and clean)
        if (mod == 0 && t.getSecond() == 0 && t.getNano() == 0) return t;

        int add = (mod == 0) ? 0 : (slotMinutes - mod);
        return t.plusMinutes(add).withSecond(0).withNano(0);
    }

    private record TimeRange(LocalDateTime start, LocalDateTime end) {}
}
