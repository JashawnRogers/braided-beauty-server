package com.braided_beauty.braided_beauty.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "refresh_tokens",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_refresh_token_token_hash", columnNames = "token_hash")
        },
        indexes = {
                @Index(name = "ix_refresh_token_user_id", columnList = "user_id"),
                @Index(name = "ix_refresh_token_family_id", columnList = "family_id"),
                @Index(name = "ix_refresh_token_expires_at", columnList = "expires_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue
    @Column(name = "token_id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private UUID tokenId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @NotNull
    @Column(name = "token_hash", nullable = false, unique = true, length = 43) // SHA-256 Base64Url
    private String tokenHash;

    @NotNull
    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "replaced_by_token_hash", length = 43)
    private String replacedByTokenHash;

    @Column(name = "device_info", length = 256)
    private String deviceInfo;

    @PrePersist
    void prePersist() {
        if (familyId == null) {
            familyId = UUID.randomUUID();
        }

        if (issuedAt == null) {
            issuedAt = Instant.now();
        }
    }
}