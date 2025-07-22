package com.braided_beauty.braided_beauty.mappers.payment;

import com.braided_beauty.braided_beauty.dtos.payment.PaymentIntentRequestDTO;
import com.braided_beauty.braided_beauty.dtos.payment.PaymentIntentResponseDTO;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PaymentDtoMapper {

    public PaymentIntentCreateParams toStripeParams(PaymentIntentRequestDTO dto){
        return PaymentIntentCreateParams.builder()
                .setAmount(dto.getAmount().multiply(BigDecimal.valueOf(100)).longValue())
                .setCurrency(dto.getCurrency())
                .setReceiptEmail(dto.getReceiptEmail())
                .build();
    }

    public PaymentIntentResponseDTO toDto(PaymentIntent intent){
        return PaymentIntentResponseDTO.builder()
                .clientSecret(intent.getClientSecret())
                .paymentIntentId(intent.getId())
                .build();
    }
}
