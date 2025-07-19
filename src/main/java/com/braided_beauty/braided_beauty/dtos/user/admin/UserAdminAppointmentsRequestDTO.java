package com.braided_beauty.braided_beauty.dtos.user.admin;

import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class UserAdminAppointmentsRequestDTO {
    @NotNull
    private final UUID appointmentId;
    @NotNull
    private final UUID serviceId;
    @NotNull
    private final LocalDateTime appointmentTime;
    @NotNull
    private final AppointmentStatus appointmentStatus;
    private final String notes;
}
