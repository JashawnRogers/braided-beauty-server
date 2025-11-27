package com.braided_beauty.braided_beauty.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loyalty_records")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LoyaltyRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore
    private User user;

    private Integer points = 0;

    @Column(name = "redeemed_points")
    private Integer redeemedPoints = 0;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "enabled")
    private Boolean enabled = true;

    @Column(name = "sign_up_bonus_awarded", nullable = false)
    private boolean signupBonusAwarded = false;

    @Version
    private long version;

    public LoyaltyRecord(User user) {
        this.user = user;
        this.points = 0;
        this.redeemedPoints = 0;
        this.signupBonusAwarded = false;
        this.enabled = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void addPoints(int amount) {
        if (amount < 0) throw new IllegalArgumentException("Points must be positive");
        this.points += amount;
    }

    public void redeemPoints(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("Redeemed points must be positive");
        if (this.points < amount) throw new IllegalArgumentException("Insufficient points");
        this.points -= amount;
        this.redeemedPoints += amount;
    }
}
