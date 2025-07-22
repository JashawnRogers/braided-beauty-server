package com.braided_beauty.braided_beauty.dtos.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@AllArgsConstructor
@Getter
@Setter
public class PaymentIntentResponseDTO {
    private final String clientSecret;
    private final String paymentIntentId;
}
