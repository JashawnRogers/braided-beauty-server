package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.exceptions.ConflictException;
import com.braided_beauty.braided_beauty.models.ScheduleCalendar;
import com.braided_beauty.braided_beauty.models.ServiceModel;
import com.braided_beauty.braided_beauty.repositories.AppointmentRepository;
import com.braided_beauty.braided_beauty.repositories.ScheduleCalendarDateOverrideRepository;
import com.braided_beauty.braided_beauty.repositories.ScheduleCalendarHoursRepository;
import com.braided_beauty.braided_beauty.repositories.ScheduleCalendarRepository;
import com.braided_beauty.braided_beauty.repositories.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleCalendarServiceDeleteTest {
    @Mock private ScheduleCalendarRepository scheduleCalendarRepository;
    @Mock private ScheduleCalendarHoursRepository scheduleCalendarHoursRepository;
    @Mock private ScheduleCalendarDateOverrideRepository scheduleCalendarDateOverrideRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private ServiceRepository serviceRepository;

    @InjectMocks
    private ScheduleCalendarService scheduleCalendarService;

    private UUID calendarId;
    private ScheduleCalendar defaultCalendar;

    @BeforeEach
    void setUp() {
        calendarId = UUID.randomUUID();
        defaultCalendar = new ScheduleCalendar();
        defaultCalendar.setId(UUID.randomUUID());
        defaultCalendar.setName("Default");
    }

    @Test
    void deleteCalendar_blocksDeletingDefaultCalendar() {
        ScheduleCalendar calendar = new ScheduleCalendar();
        calendar.setId(calendarId);
        calendar.setName("Default");
        when(scheduleCalendarRepository.findByIdForUpdate(calendarId)).thenReturn(Optional.of(calendar));

        assertThrows(ConflictException.class, () -> scheduleCalendarService.deleteCalendar(calendarId));

        verify(appointmentRepository, never()).existsByScheduleCalendar_Id(any());
        verify(scheduleCalendarRepository, never()).delete(any());
    }

    @Test
    void deleteCalendar_blocksWhenAppointmentsExist() {
        ScheduleCalendar calendar = new ScheduleCalendar();
        calendar.setId(calendarId);
        calendar.setName("Secondary");
        when(scheduleCalendarRepository.findByIdForUpdate(calendarId)).thenReturn(Optional.of(calendar));
        when(appointmentRepository.existsByScheduleCalendar_Id(calendarId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> scheduleCalendarService.deleteCalendar(calendarId));

        verify(serviceRepository, never()).saveAll(any());
        verify(scheduleCalendarRepository, never()).delete(any());
    }

    @Test
    void deleteCalendar_reassignsServicesAndDeletesCalendar() {
        ScheduleCalendar calendar = new ScheduleCalendar();
        calendar.setId(calendarId);
        calendar.setName("Secondary");

        ServiceModel service1 = new ServiceModel();
        service1.setScheduleCalendar(calendar);
        ServiceModel service2 = new ServiceModel();
        service2.setScheduleCalendar(calendar);

        when(scheduleCalendarRepository.findByIdForUpdate(calendarId)).thenReturn(Optional.of(calendar));
        when(appointmentRepository.existsByScheduleCalendar_Id(calendarId)).thenReturn(false);
        when(scheduleCalendarRepository.findByNameIgnoreCase("Default")).thenReturn(Optional.of(defaultCalendar));
        when(serviceRepository.findAllByScheduleCalendar_Id(calendarId)).thenReturn(List.of(service1, service2));

        scheduleCalendarService.deleteCalendar(calendarId);

        verify(serviceRepository).saveAll(argThat(services -> {
            for (ServiceModel service : services) {
                if (service.getScheduleCalendar() != defaultCalendar) {
                    return false;
                }
            }
            return true;
        }));
        verify(scheduleCalendarHoursRepository).deleteAllByCalendar_Id(calendarId);
        verify(scheduleCalendarDateOverrideRepository).deleteAllByCalendar_Id(calendarId);
        verify(scheduleCalendarRepository).delete(calendar);
    }
}
