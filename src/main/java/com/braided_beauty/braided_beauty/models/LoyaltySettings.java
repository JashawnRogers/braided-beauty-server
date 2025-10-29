package com.braided_beauty.braided_beauty.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "loyalty_settings")
@Getter
@Setter
public class LoyaltySettings {
    // Seed on startup
    public static final UUID SINGLETON_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Id
    private UUID id = SINGLETON_ID;

    @Column(name = "program_enabled", nullable = false)
    private boolean programEnabled = true;

    @Column(name = "signup_bonus_points", nullable = false)
    private Integer signUpBonusPoints = 0;

    @Column(name = "earn_per_appointment")
    private Integer earnPerAppointment = 0;
}
