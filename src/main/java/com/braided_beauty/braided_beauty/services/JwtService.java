package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.records.BookingConfirmationToken;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

@Service
@AllArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;

    public String generateAccessToken(UUID userId, String email, String name, Collection< ? extends GrantedAuthority> authorities) {
        Instant now = Instant.now();

        // previously used "scope" which can be processed by spring security but is mainly meant for OAuth2 flows
        // Now using roles for a more universal approach since I will be using trad email/password and OAuth2 given that spring can get authorities from either flow
        var roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        var claims = JwtClaimsSet.builder()
                .issuer("braided-beauty")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(15 * 60)) // 15 minutes
                .subject(userId.toString())
                .claim("email", email)
                .claim("name", name != null ? name : email) // Name is not required but nice to have, so fallback is email that is required.
                .claim("roles", roles)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public BookingConfirmationToken generateBookingConfirmationToken(UUID appointmentId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(60 * 60 * 24);
        String jti = UUID.randomUUID().toString();

        var claims = JwtClaimsSet.builder()
                .issuer("braided-beauty")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(appointmentId.toString())
                .claim("typ", "booking_confirmation")
                .id(jti)
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return new BookingConfirmationToken(token, jti, expiresAt);
    }
}
