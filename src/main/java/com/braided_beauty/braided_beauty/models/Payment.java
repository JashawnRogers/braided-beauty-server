package com.braided_beauty.braided_beauty.models;

import com.braided_beauty.braided_beauty.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Payment {
    @Id
    @GeneratedValue
    private UUID id;
    @Column(name = "stripe_session_id")
    private String stripeSessionId;
    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;
    @Column(name = "amount")
    private BigDecimal amount;
    @Column(name = "tip_amount")
    private BigDecimal tipAmount;
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus;

    @ManyToOne
    private Appointment appointment;
    @ManyToOne
    private User user;
}
