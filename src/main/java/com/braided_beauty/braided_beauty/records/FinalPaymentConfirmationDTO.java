package com.braided_beauty.braided_beauty.records;

import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import com.braided_beauty.braided_beauty.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record FinalPaymentConfirmationDTO(
        UUID appointmentId,
        String serviceName,
        BigDecimal depositAmount,
        BigDecimal tipAmount,
        BigDecimal totalAmount,
        BigDecimal remainingBalance
) {
}
