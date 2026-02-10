package com.braided_beauty.braided_beauty.models;

import com.braided_beauty.braided_beauty.records.DiscountType;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(
        name = "promo_codes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_promo_code", columnNames = "code")
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PromoCode {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "code", nullable = false, length = 64)
    private String code; // Store uppercase

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType; // PERCENT | AMOUNT

    @Column(name = "value", nullable = false, precision = 10, scale = 2)
    private BigDecimal value; // Store both percent and amount as decimal

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "max_redemptions")
    private Integer maxRedemptions;

    @Column(name = "times_redeemed", nullable = false)
    private Integer timesRedeemed = 0;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        if (code != null) {
            code = code.trim().toUpperCase(Locale.ROOT);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();

        if (code != null) {
            code = code.trim().toUpperCase(Locale.ROOT);
        }
    }
}
