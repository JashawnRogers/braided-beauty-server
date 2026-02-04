package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.timeSlots.AvailableTimeSlotsDTO;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.models.*;
import com.braided_beauty.braided_beauty.repositories.AppointmentRepository;
import com.braided_beauty.braided_beauty.repositories.BusinessHoursRepository;
import com.braided_beauty.braided_beauty.repositories.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AvailabilityService {
    private final ServiceRepository serviceRepository;
    private final BusinessHoursRepository businessHoursRepository;
    private final AppointmentRepository appointmentRepository;
    private final BusinessSettingsService businessSettingsService;
    private final AddOnService addOnService;


    public List<AvailableTimeSlotsDTO> getAvailability(UUID serviceId, LocalDate date, List<UUID> addOnIds) {
        ServiceModel service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new NotFoundException("Service not found with ID: " + serviceId));

        BusinessHours workingDay = businessHoursRepository.findByDayOfWeek(date.getDayOfWeek())
                .orElseThrow(() -> new NotFoundException("No business hours set for " + date.getDayOfWeek()));

        if (workingDay.isClosed()) return List.of();

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

        LocalDateTime open = LocalDateTime.of(date, workingDay.getOpenTime());
        LocalDateTime close = LocalDateTime.of(date, workingDay.getCloseTime());

        if (date.equals(LocalDate.now())) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime minStart = ceilToSlot(now, slotMinutes);
            if (minStart.isAfter(open)) {
                open = minStart;
            }
        }

        // If single stylist: block against ALL appointments, not by serviceId.
        List<Appointment> appointments =
                appointmentRepository.findBlockingAppointmentsForWindow(open, close);

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
