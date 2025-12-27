package com.braided_beauty.braided_beauty.dtos.appointment;

import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import com.braided_beauty.braided_beauty.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
@AllArgsConstructor
@Getter
@Setter
public class AdminAppointmentSummaryDTO {
    private final UUID appointmentId;
    private final LocalDateTime appointmentTime;
    private final AppointmentStatus appointmentStatus;
    private final String serviceName;
    private final List<String> addOns;
    private final PaymentStatus paymentStatus;
    private final BigDecimal remainingBalance;
    private final BigDecimal totalAmount;
}
