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

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "enabled")
    private Boolean enabled = true;

    @Column(name = "sign_up_bonus_awarded", nullable = false)
    private boolean signupBonusAwarded = false;

//    @Column(name = "free_reschedule_credits")
//    private int freeRescheduleCredits = 1;

    @Version
    private long version;

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
