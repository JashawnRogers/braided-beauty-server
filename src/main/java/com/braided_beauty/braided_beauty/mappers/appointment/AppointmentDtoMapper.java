package com.braided_beauty.braided_beauty.mappers.appointment;

import com.braided_beauty.braided_beauty.dtos.appointment.AdminAppointmentSummaryDTO;
import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentRequestDTO;
import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentResponseDTO;
import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentSummaryDTO;
import com.braided_beauty.braided_beauty.mappers.service.ServiceDtoMapper;
import com.braided_beauty.braided_beauty.models.AddOn;
import com.braided_beauty.braided_beauty.models.Appointment;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
        var service = serviceDtoMapper.toDto(appointment.getService());
        String email = appointment.getUser() != null ? appointment.getUser().getEmail() : appointment.getGuestEmail();
        String guestCancelToken = appointment.getUser() == null ? appointment.getGuestCancelToken() : null;

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
                .addOns(appointment.getAddOns())
                .email(email)
                .guestCancelToken(guestCancelToken)
                .build();
    }

    public AppointmentSummaryDTO toSummaryDTO(Appointment appointment) {
        return AppointmentSummaryDTO.builder()
                .id(appointment.getId())
                .serviceName(appointment.getService().getName())
                .appointmentTime(appointment.getAppointmentTime())
                .appointmentStatus(appointment.getAppointmentStatus())
                .build();
    }

    public AdminAppointmentSummaryDTO toAdminSummaryDTO(Appointment appointment) {
        UUID serviceId = appointment.getService().getId();
        String serviceName = appointment.getService().getName();
        BigDecimal deposit = Optional.ofNullable(appointment.getDepositAmount()).orElse(BigDecimal.ZERO);
        BigDecimal tip = Optional.ofNullable(appointment.getTipAmount()).orElse(BigDecimal.ZERO);
        BigDecimal servicePrice = Optional.ofNullable(appointment.getService().getPrice()).orElse(BigDecimal.ZERO);

        List<UUID> addOnIds = appointment.getAddOns().stream()
                .map(AddOn::getId)
                .filter(Objects::nonNull)
                .toList();

        BigDecimal totalPriceOfAddOns =
                appointment.getAddOns().stream()
                        .map(AddOn::getPrice)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainingBalance = servicePrice
                        .add(totalPriceOfAddOns)
                        .subtract(deposit);

        BigDecimal totalAmount = servicePrice
                .add(totalPriceOfAddOns)
                .add(tip);

        List<String> addOns = appointment.getAddOns()
                .stream()
                .map(AddOn::getName)
                .toList();

        String customerName = appointment.getUser().getName() != null ?
                appointment.getUser().getName() :
                "Guest";

        String customerEmail = appointment.getGuestEmail() != null ?
                appointment.getGuestEmail() :
                appointment.getUser().getEmail();

        return AdminAppointmentSummaryDTO.builder()
                .id(appointment.getId())
                .appointmentTime(appointment.getAppointmentTime())
                .appointmentStatus(appointment.getAppointmentStatus())
                .serviceId(serviceId)
                .serviceName(serviceName)
                .addOnIds(addOnIds)
                .paymentStatus(appointment.getPaymentStatus())
                .remainingBalance(remainingBalance)
                .totalAmount(totalAmount)
                .customerName(customerName)
                .customerEmail(customerEmail)
                .tipAmount(appointment.getTipAmount())
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .note(appointment.getNote())
                .build();
    }
}
