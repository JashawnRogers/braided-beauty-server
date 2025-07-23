package com.braided_beauty.braided_beauty.mappers.appointment;

import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentRequestDTO;
import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentResponseDTO;
import com.braided_beauty.braided_beauty.dtos.service.ServiceResponseDTO;
import com.braided_beauty.braided_beauty.models.Appointment;
import org.springframework.stereotype.Component;

@Component
public class AppointmentDtoMapper {

    public Appointment toEntity(AppointmentRequestDTO dto){
        return Appointment.builder()
                .appointmentTime(dto.getAppointmentTime())
                .note(dto.getNote())
                .stripePaymentId(dto.getStripePaymentId())
                .build();
    }

    public AppointmentResponseDTO toDTO(Appointment appointment,
                                                           ServiceResponseDTO service){
        return AppointmentResponseDTO.builder()
                .id(appointment.getId())
                .appointmentTime(appointment.getAppointmentTime())
                .appointmentStatus(appointment.getAppointmentStatus())
                .createdAt(appointment.getCreatedAt())
                .service(service)
                .depositAmount(appointment.getDepositAmount())
                .paymentStatus(appointment.getPaymentStatus())
                .stripePaymentId(appointment.getStripePaymentId())
                .pointsEarned(service.getPointsEarned())
                .updatedAt(appointment.getUpdatedAt())
                .note(appointment.getNote())
                .build();
    }
}
