package com.braided_beauty.braided_beauty.dtos.appointment;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class AppointmentRequestDTO {
    @NotNull(message = "Appointment must have a time.")
    private final LocalDateTime appointmentTime;
    @NotNull(message = "appointment must be linked to a service.")
    private final UUID serviceId;
    @Email
    private final String receiptEmail;
    private final String note;
    private final String stripePaymentId;
    private final String cancelReason;
    private final List<UUID> addOnIds;
    private final String promoText;
}
