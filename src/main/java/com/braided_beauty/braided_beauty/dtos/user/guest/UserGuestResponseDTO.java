package com.braided_beauty.braided_beauty.dtos.user.guest;

import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentResponseDTO;
import com.braided_beauty.braided_beauty.enums.UserType;
import com.braided_beauty.braided_beauty.models.Appointment;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class UserGuestResponseDTO {
    @NotNull
    private final UUID id;
    @NotNull
    private final String name;
    @NotNull
    private final UserType userType;
    @NotNull
    private final LocalDateTime createdAt;
    @NotNull
    private final AppointmentResponseDTO appointment;
}
