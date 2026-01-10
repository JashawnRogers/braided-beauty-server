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
    private final UUID id;
    private final LocalDateTime appointmentTime;
    private final AppointmentStatus appointmentStatus;
    private final UUID serviceId;
    private final String serviceName;
    private final List<UUID> addOnIds;
    private final PaymentStatus paymentStatus;
    private final BigDecimal remainingBalance;
    private final BigDecimal totalAmount;
    private final String customerName;
    private final String customerEmail;
    private final BigDecimal tipAmount;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final String note;
}
