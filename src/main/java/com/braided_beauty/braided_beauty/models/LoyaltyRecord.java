package com.braided_beauty.braided_beauty.models;

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
    @GeneratedValue
    private UUID id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id", nullable = false ,unique = true)
    private User user;

    private Integer points = 0;

    @Column(name = "redeemed_points")
    private Integer redeemedPoints = 0;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate(){
        updatedAt = LocalDateTime.now();
    }
}
