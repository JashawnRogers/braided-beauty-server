package com.braided_beauty.braided_beauty.mappers.appointment;

import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentRequestDTO;
import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentResponseDTO;
import com.braided_beauty.braided_beauty.dtos.service.ServiceResponseDTO;
import com.braided_beauty.braided_beauty.models.Appointment;
import com.braided_beauty.braided_beauty.models.Service;
import org.springframework.stereotype.Component;

@Component
public class AppointmentDtoMapper {

    public Appointment toEntity(AppointmentRequestDTO dto){
        return Appointment.builder()
                .appointmentTime(dto.getAppointmentTime())
                .id(dto.getServiceId())
                .note(dto.getNote())
                .build();
    }

    public AppointmentResponseDTO toAppointmentResponseDTO(Appointment appointment,
                                                           ServiceResponseDTO service,
                                                           AppointmentResponseDTO dto){
        return AppointmentResponseDTO.builder()
                .id(appointment.getId())
                .appointmentTime(appointment.getAppointmentTime())
                .appointmentStatus(appointment.getAppointmentStatus())
                .createdAt(appointment.getCreatedAt())
                .service(service)
                .pointsEarned(dto.getPointsEarned())
                .updatedAt(appointment.getUpdatedAt())
                .note(appointment.getNote())
                .build();
    }
}
