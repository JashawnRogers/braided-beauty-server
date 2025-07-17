package com.braided_beauty.braided_beauty.dtos.user.guest;

import com.braided_beauty.braided_beauty.enums.UserType;
import com.braided_beauty.braided_beauty.models.Appointment;
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
    private final UUID id;
    private final String name;
    private final UserType userType;
    private final LocalDateTime createdAt;
    private final List<Appointment> appointments; // Change to GuestAppointmentDTO once it is created
}
