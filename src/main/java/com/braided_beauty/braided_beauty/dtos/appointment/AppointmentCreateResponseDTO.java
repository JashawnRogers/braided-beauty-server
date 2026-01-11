package com.braided_beauty.braided_beauty.dtos.appointment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Builder
@Getter
@AllArgsConstructor
public class AppointmentCreateResponseDTO {
    private UUID appointmentId;
    private boolean paymentRequired;
    private String checkoutUrl;
    private String confirmationToken;
}
