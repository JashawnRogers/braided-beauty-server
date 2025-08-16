package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.refreshToken.IssuedRefreshTokenDTO;
import com.braided_beauty.braided_beauty.dtos.refreshToken.RotateTokensDTO;
import com.braided_beauty.braided_beauty.enums.UserType;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.exceptions.UnauthorizedException;
import com.braided_beauty.braided_beauty.models.RefreshToken;
import com.braided_beauty.braided_beauty.models.User;
import com.braided_beauty.braided_beauty.records.AppUserPrincipal;
import com.braided_beauty.braided_beauty.repositories.RefreshTokenRepository;
import com.braided_beauty.braided_beauty.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

@Service
@AllArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final Clock clock;

    private static final Duration REFRESH_TTL = Duration.ofDays(14);
    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();

    @Transactional
    public IssuedRefreshTokenDTO issueForUser(UUID userId, String deviceInfo){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));
        String raw = generateOpaqueToken(); // Generate token
        String hash = sha256(raw); // Create one way hash of token to store in DB
        Instant now = Instant.now(clock);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hash)
                .issuedAt(now)
                .expiresAt(now.plus(REFRESH_TTL))
                .familyId(UUID.randomUUID())
                .deviceInfo(deviceInfo)
                .build();

        refreshTokenRepository.save(refreshToken);

        return IssuedRefreshTokenDTO.builder()
                .refreshToken(raw)
                .expiresAt(refreshToken.getExpiresAt())
                .build();
    }

    @Transactional
    public RotateTokensDTO rotate(String presentedToken) {
        String hash = sha256(presentedToken); // Hash token to compare to DB
        RefreshToken currentToken = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token."));

        Instant now = Instant.now(clock);

        boolean reused = currentToken.getRevokedAt() != null;
        boolean expired = now.isAfter(currentToken.getExpiresAt());

        if (reused){
            refreshTokenRepository.revokeFamily(currentToken.getFamilyId(), now);
            throw new UnauthorizedException("Refresh token reuse detected.");
        }
        if (expired) {
            throw new UnauthorizedException("Refresh token expired.");
        }

        // Revoke current token
        currentToken.setRevokedAt(now);
        refreshTokenRepository.save(currentToken);

        // Issue next refresh token in same family
        String newRaw = generateOpaqueToken();
        String newHash = sha256(newRaw);

        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(currentToken.getUser())
                .tokenHash(newHash)
                .issuedAt(now)
                .expiresAt(now.plus(REFRESH_TTL))
                .familyId(currentToken.getFamilyId())
                .deviceInfo(currentToken.getDeviceInfo())
                .build();
        refreshTokenRepository.save(newRefreshToken);

        currentToken.setReplacedByTokenHash(newHash);
        refreshTokenRepository.save(currentToken);

        // Create new access token for user
        var auth = buildAuthForUser(currentToken.getUser().getId());
        String newAccessToken = jwtService.generateAccessToken(auth);

        return RotateTokensDTO.builder()
                .newAccessToken(newAccessToken)
                .newRefreshToken(newRaw)
                .refreshExpiresAt(newRefreshToken.getExpiresAt())
                .build();
    }

    @Transactional
    public void revoke(String presentedToken) {
        String hash = sha256(presentedToken);
        refreshTokenRepository.revokeByHash(hash, Instant.now(clock));
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.revokeAllForUser(userId, Instant.now(clock));
    }

    @Transactional
    public int purgeExpired() {
        return refreshTokenRepository.deleteAllByExpiresAtBefore(Instant.now(clock));
    }


    // This is the algo to generate the actual refresh token that will be given to the client
    private String generateOpaqueToken() {
        byte[] b = new byte[32]; // 32 bytes = 256 bits (of entropy) = 43 characters
        SECURE_RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    // To generate hash code of token
    private String sha256(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private Authentication buildAuthForUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));

        if (!user.isEnabled()) {
            throw new org.springframework.security.authentication.DisabledException("User is disabled");
        }

        Set<String> roleStrings = UserType.roleStringsFor(user.getUserType());
        var authorities = roleStrings.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        var principal = new AppUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getName() != null ? user.getName() : null,
                roleStrings
        );

        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }
}
