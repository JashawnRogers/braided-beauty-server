package com.braided_beauty.braided_beauty.models;

import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import com.braided_beauty.braided_beauty.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "appointments")
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

    @Column(name = "appointment_time", nullable = false, unique = true)
    private LocalDateTime appointmentTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "appointment_status",nullable = false)
    private AppointmentStatus appointmentStatus;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "deposit_amount")
    private BigDecimal depositAmount;

    @Column(name = "tip_amount")
    private BigDecimal tipAmount;

    @Column(name = "payment_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Column(name = "stripe_payment_id")
    private String stripePaymentId;

    @Column(name = "stripe_session_id")
    private String stripeSessionId;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @ManyToMany
    @JoinTable(
            name = "appointment_add_ons", // Name of the join table
            joinColumns = @JoinColumn(name = "appointment_id"), // Name of join table column and foreign key to appointment entity
            inverseJoinColumns = @JoinColumn(name = "add_on_id") //Name of join table column and foreign key to add on entity
    )
    private List<AddOn> addOns;

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
