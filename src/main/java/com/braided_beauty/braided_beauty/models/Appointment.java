package com.braided_beauty.braided_beauty.models;

import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import com.braided_beauty.braided_beauty.enums.PaymentStatus;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "appointments",
        uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_appointment_time",
                columnNames = "appointment_time"
            )
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Appointment {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "service_id")
    private ServiceModel service;

    @Column(name = "guest_email")
    private String guestEmail;

    @Column(name = "appointment_time", nullable = false, unique = true)
    private LocalDateTime appointmentTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "appointment_status",nullable = false)
    private AppointmentStatus appointmentStatus;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "deposit_amount")
    private BigDecimal depositAmount;

    @Column(name = "tip_amount")
    private BigDecimal tipAmount;

    @Column(name = "remaining_balance")
    private BigDecimal remainingBalance; // = (amount of base service + any add-ons) - deposit

    @Column(name = "total_amount")
    private BigDecimal totalAmount; // = amount of base service + any add-ons + tip

    @Column(name = "service_price_at_booking")
    private BigDecimal servicePriceAtBooking;

    @Column(name = "add_ons_total_at_booking")
    private BigDecimal addOnsTotalAtBooking;

    @Column(name = "subtotal_at_booking")
    private BigDecimal subtotalAtBooking;

    @Column(name = "post_deposit_balance_at_booking")
    private BigDecimal postDepositBalanceAtBooking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promo_code_id")
    private PromoCode promoCode;


    /**
     * Snapshot of the code the customer typed at booking time.
     * This helps keep history even if the promo code record is later edited/disabled.
     */
    @Column(name = "promo_code_text")
    private String promoCodeText;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "discount_percent")
    private Integer discountPercent = 0;

    @Column(name = "payment_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Column(name = "stripe_payment_id")
    private String stripePaymentId;

    @Column(name = "stripe_session_id")
    private String stripeSessionId;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "loyalty_applied", nullable = false)
    private boolean loyaltyApplied = false;

    @Column(name = "guest_cancel_token")
    private String guestCancelToken;

    @Column(name = "guest_token_expires_at")
    private LocalDateTime guestTokenExpiresAt;

    @Column(name = "duration_minutes")
    private int durationMinutes;

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt;

    @Column(name = "booking_confirmation_jti", length = 64)
    private String bookingConfirmationJti;

    private Instant bookingConfirmationExpiresAt;

    @ManyToMany
    @JoinTable(
            name = "appointment_add_ons", // Name of the join table
            joinColumns = @JoinColumn(name = "appointment_id"), // Name of join table column and foreign key to appointment entity
            inverseJoinColumns = @JoinColumn(name = "add_on_id") //Name of join table column and foreign key to add on entity
    )
    @Builder.Default
    private List<AddOn> addOns = new ArrayList<>();

    @PrePersist
    protected void onCreate(){
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate(){
        updatedAt = LocalDateTime.now();
    }

}
