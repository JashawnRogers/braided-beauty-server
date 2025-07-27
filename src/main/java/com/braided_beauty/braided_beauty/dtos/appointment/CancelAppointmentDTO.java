package com.braided_beauty.braided_beauty.dtos.appointment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class CancelAppointmentDTO {
    private final UUID appointmentId;
    private final UUID userId;
    private final String cancelReason;
}
