package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.models.ScheduleCalendar;
import com.braided_beauty.braided_beauty.models.ServiceModel;
import com.braided_beauty.braided_beauty.repositories.AppointmentRepository;
import com.braided_beauty.braided_beauty.repositories.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceSchedulingRulesTest {
    @Mock private ServiceRepository serviceRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private BusinessSettingsService businessSettingsService;
    @Mock private AddOnService addOnService;
    @Mock private ScheduleCalendarService scheduleCalendarService;

    @InjectMocks
    private AvailabilityService availabilityService;

    private UUID serviceId;
    private ScheduleCalendar calendar;
    private ServiceModel service;
    private LocalDate date;

    @BeforeEach
    void setUp() {
        serviceId = UUID.randomUUID();
        date = LocalDate.of(2026, 3, 12);

        calendar = new ScheduleCalendar();
        calendar.setId(UUID.randomUUID());
        calendar.setMaxBookingsPerDay(0);

        service = new ServiceModel();
        service.setId(serviceId);
        service.setDurationMinutes(60);
        service.setScheduleCalendar(calendar);

        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(service));
    }

    @Test
    void getAvailability_returnsEmptyWhenOverrideMarksDateClosed() {
        when(scheduleCalendarService.isDateWithinBookingWindow(calendar, date)).thenReturn(true);
        when(scheduleCalendarService.getEffectiveCalendarConstraints(calendar, date))
                .thenReturn(new ScheduleCalendarService.EffectiveCalendarConstraints(
                        true, null, null, null, null
                ));

        assertTrue(availabilityService.getAvailability(serviceId, date, null).isEmpty());
    }

    @Test
    void getAvailability_returnsEmptyWhenDailyCapReached() {
        calendar.setMaxBookingsPerDay(1);
        when(scheduleCalendarService.isDateWithinBookingWindow(calendar, date)).thenReturn(true);
        when(scheduleCalendarService.getEffectiveCalendarConstraints(calendar, date))
                .thenReturn(new ScheduleCalendarService.EffectiveCalendarConstraints(
                        false,
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0),
                        LocalDateTime.of(2026, 3, 12, 9, 0),
                        LocalDateTime.of(2026, 3, 12, 18, 0)
                ));
        when(appointmentRepository.countBlockingBookingsForDay(any(), any(), eq(calendar.getId()))).thenReturn(1L);

        assertTrue(availabilityService.getAvailability(serviceId, date, null).isEmpty());

        verify(appointmentRepository).countBlockingBookingsForDay(any(), any(), eq(calendar.getId()));
    }

    @Test
    void getAvailability_marksSlotUnavailableWhenEarlierAppointmentStillOverlapsViaBuffer() {
        when(scheduleCalendarService.isDateWithinBookingWindow(calendar, date)).thenReturn(true);
        when(scheduleCalendarService.getEffectiveCalendarConstraints(calendar, date))
                .thenReturn(new ScheduleCalendarService.EffectiveCalendarConstraints(
                        false,
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0),
                        LocalDateTime.of(2026, 3, 12, 9, 0),
                        LocalDateTime.of(2026, 3, 12, 18, 0)
                ));
        when(businessSettingsService.getAppointmentBufferMinutes()).thenReturn(60);

        com.braided_beauty.braided_beauty.models.Appointment existing =
                com.braided_beauty.braided_beauty.models.Appointment.builder()
                        .appointmentTime(LocalDateTime.of(2026, 3, 12, 13, 30))
                        .durationMinutes(60)
                        .build();

        when(appointmentRepository.findBlockingAppointmentsForWindow(
                eq(LocalDateTime.of(2026, 3, 12, 9, 0)),
                eq(LocalDateTime.of(2026, 3, 12, 18, 0)),
                eq(60),
                eq(calendar.getId())
        )).thenReturn(List.of(existing));

        boolean twoThirtyAvailable = availabilityService.getAvailability(serviceId, date, null).stream()
                .filter(slot -> "14:30".equals(slot.time()))
                .findFirst()
                .orElseThrow()
                .available();

        assertFalse(twoThirtyAvailable);
    }
}
