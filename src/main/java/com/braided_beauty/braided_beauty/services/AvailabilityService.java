package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.config.SchedulingConfig;
import com.braided_beauty.braided_beauty.dtos.timeSlots.AvailableTimeSlotsDTO;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.models.Appointment;
import com.braided_beauty.braided_beauty.models.BusinessHours;
import com.braided_beauty.braided_beauty.models.ServiceModel;
import com.braided_beauty.braided_beauty.repositories.AppointmentRepository;
import com.braided_beauty.braided_beauty.repositories.BusinessHoursRepository;
import com.braided_beauty.braided_beauty.repositories.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvailabilityService {
    private final ServiceRepository serviceRepository;
    private final BusinessHoursRepository businessHoursRepository;
    private final AppointmentRepository appointmentRepository;
    private final SchedulingConfig schedulingConfig;


    public List<AvailableTimeSlotsDTO> getAvailability(UUID serviceId, LocalDate date) {
        ServiceModel service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new NotFoundException("Service not found with ID: " + serviceId));

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        BusinessHours workingDay = businessHoursRepository.findByDayOfWeek(dayOfWeek)
                .orElseThrow(() -> new NotFoundException("No business hours set for " + dayOfWeek));

        if (workingDay.isClosed()) {
            return List.of();
        }

        int serviceDurationInMinutes = service.getDurationMinutes();
        int bufferMinutes = schedulingConfig.getBufferMinutes();
        int slotMinutes = 30;

        LocalDateTime dayOpenTime = LocalDateTime.of(date, workingDay.getOpenTime());
        LocalDateTime dayCloseTime = LocalDateTime.of(date, workingDay.getCloseTime());

        // Pull only appointments that could block that day
        List<Appointment> appointments = appointmentRepository
                .findByServiceIdAndAppointmentTimeBetween(serviceId, dayOpenTime, dayCloseTime);

        // Convert appointments into blocked intervals
        List<TimeRange> blocked = appointments.stream()
                .map(a -> {
                    LocalDateTime start = a.getAppointmentTime();
                    LocalDateTime end = start.plusMinutes(a.getService().getDurationMinutes());
                    return new TimeRange(start, end.plusMinutes(bufferMinutes));
                })
                .toList();

        List<AvailableTimeSlotsDTO> availableSlots = new ArrayList<>();

        LocalDateTime latestStartTime = dayCloseTime.minusMinutes(serviceDurationInMinutes + bufferMinutes);

        for (LocalDateTime slotStart = dayOpenTime; !slotStart.isAfter(latestStartTime); slotStart = slotStart.plusMinutes(slotMinutes)) {
            LocalDateTime slotEnd = slotStart.plusMinutes(serviceDurationInMinutes + bufferMinutes);

            LocalDateTime finalSlotStart = slotStart;
            boolean isBlocked = blocked.stream().anyMatch(b -> isOverlap(finalSlotStart, slotEnd, b.start, b.end));
            boolean available = !isBlocked;

            availableSlots.add(
                    new AvailableTimeSlotsDTO(
                            slotStart.toLocalTime().toString(),
                            available,
                            slotStart,
                            slotEnd
                    )
                );
        }

        return availableSlots;

    }

    private boolean isOverlap(LocalDateTime aStart, LocalDateTime aEnd, LocalDateTime bStart, LocalDateTime bEnd){
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    private record TimeRange (LocalDateTime start, LocalDateTime end) {}
}
