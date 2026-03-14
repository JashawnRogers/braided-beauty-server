package com.braided_beauty.braided_beauty.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointment_fees")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Setter
@Builder
public class AppointmentFee {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_id")
    private Fee fee;

    @Column(name = "fee_name", nullable = false, length = 100)
    private String feeName;

    @Column(name = "fee_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();;
    }
}
