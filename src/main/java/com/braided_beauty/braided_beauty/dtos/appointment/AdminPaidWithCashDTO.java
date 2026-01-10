package com.braided_beauty.braided_beauty.dtos.appointment;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@AllArgsConstructor
@Getter
public class AdminPaidWithCashDTO {
    public final UUID appointmentId;
    public final BigDecimal tipAmount;
    public final String note;
}
