package com.braided_beauty.braided_beauty.records;

import com.braided_beauty.braided_beauty.dtos.addOn.AddOnResponseDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ConfirmationReceiptDTO(
        UUID appointmentId,
        String serviceName,
        BigDecimal servicePrice,
        LocalDateTime appointmentTime,
        int durationMinutes,
        BigDecimal depositAmount,
        BigDecimal remainingBalance,
        BigDecimal tipAmount,
        List<AddOnResponseDTO> addOns,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        Integer discountPercent
) {
}
