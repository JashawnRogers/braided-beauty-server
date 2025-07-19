package com.braided_beauty.braided_beauty.dtos.user.admin;

import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class UserAdminAppointmentResponseDTO {
    private final UUID id;
    private final UUID serviceId;
    private final LocalDateTime appointmentTime;
    private final AppointmentStatus appointmentStatus;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final String notes;
}
