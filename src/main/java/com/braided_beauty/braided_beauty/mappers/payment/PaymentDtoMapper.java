package com.braided_beauty.braided_beauty.mappers.payment;

import com.braided_beauty.braided_beauty.dtos.payment.PaymentIntentRequestDTO;
import com.braided_beauty.braided_beauty.dtos.payment.PaymentIntentResponseDTO;
import com.braided_beauty.braided_beauty.dtos.payment.PaymentResponseDTO;
import com.braided_beauty.braided_beauty.mappers.appointment.AppointmentDtoMapper;
import com.braided_beauty.braided_beauty.mappers.user.admin.UserAdminMapper;
import com.braided_beauty.braided_beauty.models.Payment;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@AllArgsConstructor
public class PaymentDtoMapper {
    private final AppointmentDtoMapper appointmentDtoMapper;
    private final UserAdminMapper userMapper;

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

    public PaymentResponseDTO toDto(Payment payment){
        return PaymentResponseDTO.builder()
                .id(payment.getId())
                .stripeSessionId(payment.getStripeSessionId())
                .stripePaymentIntentId(payment.getStripePaymentIntentId())
                .amount(payment.getAmount())
                .tipAmount(payment.getTipAmount())
                .paymentStatus(payment.getPaymentStatus())
                .appointment(appointmentDtoMapper.toDTO(payment.getAppointment()))
                .user(userMapper.toSummaryDTO(payment.getUser()))
                .build();
    }
}
