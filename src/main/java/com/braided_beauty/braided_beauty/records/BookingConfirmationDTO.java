package com.braided_beauty.braided_beauty.records;

import com.braided_beauty.braided_beauty.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BookingConfirmationDTO(
        UUID appointmentId,
        String serviceName,
        LocalDateTime appointmentTime,
        Integer durationMinutes,
        BigDecimal depositAmount,
        BigDecimal totalAmountRemaining,
        PaymentStatus paymentStatus
) {
}
