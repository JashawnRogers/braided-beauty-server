package com.braided_beauty.braided_beauty.dtos.appointment;

import com.braided_beauty.braided_beauty.dtos.service.ServiceResponseDTO;
import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import com.braided_beauty.braided_beauty.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@AllArgsConstructor
@Getter
@Setter
public class AppointmentResponseDTO {
    private final UUID id;
    private final LocalDateTime appointmentTime;
    private final AppointmentStatus appointmentStatus;
    private final LocalDateTime createdAt;
    private final ServiceResponseDTO service;
    private final BigDecimal depositAmount;
    private final PaymentStatus paymentStatus;
    private final String stripePaymentId;
    private final Integer pointsEarned;
    private final LocalDateTime updatedAt;
    private final String note;
}
