package com.braided_beauty.braided_beauty.dtos.appointment;

import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@RequiredArgsConstructor
@Getter
@Setter
public class AppointmentSummaryDTO {
    private final UUID id;
    private final LocalDateTime appointmentTime;
    private final AppointmentStatus appointmentStatus;
    private final String serviceName;

}
