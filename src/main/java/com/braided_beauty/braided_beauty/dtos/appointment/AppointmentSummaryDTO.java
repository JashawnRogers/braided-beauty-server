package com.braided_beauty.braided_beauty.dtos.appointment;

import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
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
