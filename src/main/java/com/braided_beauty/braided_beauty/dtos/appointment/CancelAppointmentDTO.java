package com.braided_beauty.braided_beauty.dtos.appointment;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class CancelAppointmentDTO {
    @NotNull(message = "Must provide appointment ID.")
    private final UUID appointmentId;
    @Nullable
    private final String cancelReason;
}
