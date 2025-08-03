package com.braided_beauty.braided_beauty.dtos.payment;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Builder
@AllArgsConstructor
@Getter
@Setter
public class PaymentIntentRequestDTO {
    @NotNull
    private final BigDecimal amount;
    @NotNull
    private final String currency;
    @NotNull
    private final String receiptEmail;
    @NotNull
    private final Map<String, String> metadata;

}
