package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentRequestDTO;
import com.braided_beauty.braided_beauty.exceptions.BadRequestException;
import com.braided_beauty.braided_beauty.exceptions.ConflictException;
import com.braided_beauty.braided_beauty.mappers.appointment.AppointmentDtoMapper;
import com.braided_beauty.braided_beauty.models.Appointment;
import com.braided_beauty.braided_beauty.models.ScheduleCalendar;
import com.braided_beauty.braided_beauty.models.ServiceModel;
import com.braided_beauty.braided_beauty.records.FrontendProps;
import com.braided_beauty.braided_beauty.repositories.AppointmentRepository;
import com.braided_beauty.braided_beauty.repositories.ScheduleCalendarRepository;
import com.braided_beauty.braided_beauty.repositories.ServiceRepository;
import com.braided_beauty.braided_beauty.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceSchedulingRulesTest {
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private AppointmentDtoMapper appointmentDtoMapper;
    @Mock private UserRepository userRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private AddOnService addOnService;
    @Mock private PaymentService paymentService;
    @Mock private AppointmentConfirmationService appointmentConfirmationService;
    @Mock private BusinessSettingsService businessSettingsService;
    @Mock private EmailService emailService;
    @Mock private EmailTemplateService emailTemplateService;
    @Mock private PricingService pricingService;
    @Mock private PromoCodeValidationService promoCodeValidationService;
    @Mock private ScheduleCalendarService scheduleCalendarService;
    @Mock private ScheduleCalendarRepository scheduleCalendarRepository;
    @Mock private FrontendProps frontendProps;

    @InjectMocks
    private AppointmentService appointmentService;

    private UUID serviceId;
    private ScheduleCalendar calendar;
    private ServiceModel service;
    private LocalDateTime appointmentStart;
    private AppointmentRequestDTO request;
    private Appointment mappedAppointment;

    @BeforeEach
    void setUp() {
        serviceId = UUID.randomUUID();
        appointmentStart = LocalDateTime.of(2026, 3, 12, 10, 0);

        calendar = new ScheduleCalendar();
        calendar.setId(UUID.randomUUID());
        calendar.setMaxBookingsPerDay(0);

        service = new ServiceModel();
        service.setId(serviceId);
        service.setDurationMinutes(60);
        service.setScheduleCalendar(calendar);

        request = new AppointmentRequestDTO(
                appointmentStart,
                serviceId,
                "guest@example.com",
                "note",
                null,
                null,
                null,
                null
        );

        mappedAppointment = new Appointment();
        mappedAppointment.setAppointmentTime(appointmentStart);

        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(service));
        when(appointmentDtoMapper.toEntity(request)).thenReturn(mappedAppointment);
        when(scheduleCalendarRepository.findByIdForUpdate(calendar.getId())).thenReturn(Optional.of(calendar));
    }

    @Test
    void createAppointment_rejectsWhenOutsideBookingWindow() {
        when(scheduleCalendarService.isWithinBookingWindow(calendar, appointmentStart)).thenReturn(false);

        assertThrows(BadRequestException.class, () -> appointmentService.createAppointment(request, null));

        verify(appointmentRepository, never()).findConflictingAppointments(any(), any(), anyInt(), any());
    }

    @Test
    void createAppointment_rejectsWhenAppointmentWouldEndAfterClose() {
        when(scheduleCalendarService.isWithinBookingWindow(calendar, appointmentStart)).thenReturn(true);
        when(businessSettingsService.getAppointmentBufferMinutes()).thenReturn(15);
        when(scheduleCalendarService.getEffectiveCalendarConstraints(eq(calendar), any(LocalDate.class)))
                .thenReturn(new ScheduleCalendarService.EffectiveCalendarConstraints(
                        false,
                        LocalTime.of(9, 0),
                        LocalTime.of(10, 30),
                        LocalDateTime.of(2026, 3, 12, 9, 0),
                        LocalDateTime.of(2026, 3, 12, 10, 30)
                ));

        assertThrows(ConflictException.class, () -> appointmentService.createAppointment(request, null));

        verify(appointmentRepository, never()).findConflictingAppointments(any(), any(), anyInt(), any());
    }

    @Test
    void createAppointment_rejectsWhenDailyCapReached() {
        calendar.setMaxBookingsPerDay(1);

        when(scheduleCalendarService.isWithinBookingWindow(calendar, appointmentStart)).thenReturn(true);
        when(businessSettingsService.getAppointmentBufferMinutes()).thenReturn(15);
        when(scheduleCalendarService.getEffectiveCalendarConstraints(eq(calendar), any(LocalDate.class)))
                .thenReturn(new ScheduleCalendarService.EffectiveCalendarConstraints(
                        false,
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0),
                        LocalDateTime.of(2026, 3, 12, 9, 0),
                        LocalDateTime.of(2026, 3, 12, 18, 0)
                ));
        when(appointmentRepository.countBlockingBookingsForDay(any(), any(), eq(calendar.getId()))).thenReturn(1L);

        assertThrows(ConflictException.class, () -> appointmentService.createAppointment(request, null));

        verify(appointmentRepository, never()).findConflictingAppointments(any(), any(), anyInt(), any());
    }
}
