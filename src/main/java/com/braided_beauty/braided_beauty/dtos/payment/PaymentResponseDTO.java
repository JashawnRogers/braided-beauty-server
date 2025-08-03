package com.braided_beauty.braided_beauty.dtos.payment;

import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentResponseDTO;
import com.braided_beauty.braided_beauty.dtos.user.admin.UserSummaryResponseDTO;
import com.braided_beauty.braided_beauty.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
@Builder
public class PaymentResponseDTO {
    private final UUID id;
    private final String stripeSessionId;
    private final String stripePaymentIntentId;
    private final BigDecimal amount;
    private final BigDecimal tipAmount;
    private final PaymentStatus paymentStatus;
    private final AppointmentResponseDTO appointment;
    private final UserSummaryResponseDTO user;
}
