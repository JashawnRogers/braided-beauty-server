package com.braided_beauty.braided_beauty.models;

import com.braided_beauty.braided_beauty.enums.UserType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name="uk_user_oauth", columnNames={"oauth_subject"}),
                @UniqueConstraint(name="uk_user_email", columnNames={"email"})
        }
)
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class User {
    @Id
    @GeneratedValue
    private UUID id;

    private String email;

    @JsonIgnore
    private String password;

    private String name;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    private UserType userType;

    @Column(name = "oauth_provider")
    private String oauthProvider;

    @Column(name = "oauth_subject")
    private String oauthSubject;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "user")
    private List<Appointment> appointments;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, optional = true, fetch = FetchType.LAZY)
    private LoyaltyRecord loyaltyRecord;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "enabled")
    @Builder.Default
    private boolean isEnabled = true;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void applyLoyaltyRecord(LoyaltyRecord lr) {
        this.setLoyaltyRecord(lr);
        lr.setUser(this);
    }
}
