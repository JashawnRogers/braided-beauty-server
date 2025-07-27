package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.payment.PaymentIntentRequestDTO;
import com.braided_beauty.braided_beauty.dtos.payment.PaymentIntentResponseDTO;
import com.braided_beauty.braided_beauty.mappers.payment.PaymentDtoMapper;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.lang.String;

@AllArgsConstructor
@Service
public class PaymentService {
    private final PaymentDtoMapper paymentDtoMapper;
//    private String stripeApiKey;
//
//    @PostConstruct
//    public void init(){
//        Stripe.apiKey = stripeApiKey;
//    }

    public PaymentIntentResponseDTO createPaymentIntent(PaymentIntentRequestDTO dto) throws StripeException {
        PaymentIntentCreateParams createParams = paymentDtoMapper.toStripeParams(dto);
        PaymentIntent paymentIntent = PaymentIntent.create(createParams);
        return paymentDtoMapper.toDto(paymentIntent);
    }

    public void issueRefund(String paymentIntentId) throws StripeException {
        RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .build();
        Refund.create(params);
    }
}
