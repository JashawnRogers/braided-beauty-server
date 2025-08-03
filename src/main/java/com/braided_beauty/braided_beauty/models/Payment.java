package com.braided_beauty.braided_beauty.models;

import com.braided_beauty.braided_beauty.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
public class Payment {
    @Id
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
