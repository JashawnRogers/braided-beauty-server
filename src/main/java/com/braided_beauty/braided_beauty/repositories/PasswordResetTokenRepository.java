package com.braided_beauty.braided_beauty.repositories;

import com.braided_beauty.braided_beauty.models.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    @Query("""
        select t from PasswordResetToken t
        join fetch t.user
        where t.tokenHash = :tokenHash
          and t.used = false
          and t.expiresAt > :now
    """)
    Optional<PasswordResetToken> findValidToken(String tokenHash, LocalDateTime now);

    @Modifying
    @Query("""
        update PasswordResetToken t
        set t.used = true, t.usedAt = CURRENT_TIMESTAMP
        where t.user.id = :userId
          and t.used = false
    """)
    void invalidateUnusedTokensForUser(UUID userId);
}
