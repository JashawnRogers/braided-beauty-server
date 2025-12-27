package com.braided_beauty.braided_beauty.dtos.appointment;

import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Getter
public class AdminAppointmentRequestDTO {
    @NotNull
    private final UUID appointmentId;

    @Size(max = 250)
    private final String note;

    @Size(max = 250)
    private final String cancelReason;

    private final AppointmentStatus appointmentStatus;

    private final LocalDateTime appointmentTime;

    private final UUID serviceId; // In case the admin needs to adjust the service provided

    private final List<UUID> addOnIds; // In case the admin needs to add or remove add-ons

    private final BigDecimal tipAmount; // For appointments completed without Stripe
}
