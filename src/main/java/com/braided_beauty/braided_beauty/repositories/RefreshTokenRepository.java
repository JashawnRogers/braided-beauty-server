package com.braided_beauty.braided_beauty.repositories;

import com.braided_beauty.braided_beauty.models.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true) // So the EntityManager clears after the update, avoiding stale cached state
    @Query("update RefreshToken r set r.revokedAt = :ts where r.tokenHash = :hash and r.revokedAt is null")
    int revokeByHash(@Param("hash") String hash, @Param("ts") Instant ts);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken r set r.revokedAt = :ts where r.userId = :uid and r.revokedAt is null")
    int revokeAllForUser(@Param("uid") UUID uid, @Param("ts") Instant ts);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken r set r.revokedAt = :ts where r.familyId = :fid and r.revokedAt is null")
    int revokeFamily(@Param("fid") UUID fid, @Param("ts") Instant ts);

    int deleteAllByExpiresAtBefore(Instant cutoff);
}
