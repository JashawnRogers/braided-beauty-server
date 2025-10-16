package com.braided_beauty.braided_beauty.mappers.appointment;

import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentRequestDTO;
import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentResponseDTO;
import com.braided_beauty.braided_beauty.dtos.service.ServiceResponseDTO;
import com.braided_beauty.braided_beauty.mappers.service.ServiceDtoMapper;
import com.braided_beauty.braided_beauty.models.Appointment;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AppointmentDtoMapper {
    private final ServiceDtoMapper serviceDtoMapper;

    public Appointment toEntity(AppointmentRequestDTO dto){
        return Appointment.builder()
                .appointmentTime(dto.getAppointmentTime())
                .note(dto.getNote())
                .stripePaymentId(dto.getStripePaymentId())
                .build();
    }

    public AppointmentResponseDTO toDTO(Appointment appointment){
        return AppointmentResponseDTO.builder()
                .id(appointment.getId())
                .appointmentTime(appointment.getAppointmentTime())
                .appointmentStatus(appointment.getAppointmentStatus())
                .createdAt(appointment.getCreatedAt())
                .service(serviceDtoMapper.toDto(appointment.getService()))
                .depositAmount(appointment.getDepositAmount())
                .paymentStatus(appointment.getPaymentStatus())
                .stripePaymentId(appointment.getStripePaymentId())
                .pointsEarned(serviceDtoMapper.toDto(appointment.getService()).getPointsEarned())
                .updatedAt(appointment.getUpdatedAt())
                .note(appointment.getNote())
                .addOns(appointment.getAddOns())
                .build();
    }
}
