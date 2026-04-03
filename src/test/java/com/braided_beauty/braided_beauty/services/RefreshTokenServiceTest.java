package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.refreshToken.RotateTokensDTO;
import com.braided_beauty.braided_beauty.enums.UserType;
import com.braided_beauty.braided_beauty.exceptions.UnauthorizedException;
import com.braided_beauty.braided_beauty.models.RefreshToken;
import com.braided_beauty.braided_beauty.models.User;
import com.braided_beauty.braided_beauty.repositories.RefreshTokenRepository;
import com.braided_beauty.braided_beauty.repositories.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private HttpServletResponse response;

    private RefreshTokenService refreshTokenService;
    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-04-03T12:00:00Z"), ZoneOffset.UTC);
        refreshTokenService = new RefreshTokenService(
                refreshTokenRepository,
                userRepository,
                jwtService,
                clock,
                true,
                "None"
        );
    }

    @Test
    void rotate_rejectsUnknownRefreshToken() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> refreshTokenService.rotate("missing-token"));

        verify(userRepository, never()).findById(any());
        verify(jwtService, never()).generateAccessToken(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void rotate_rejectsReusedTokenAndRevokesFamily() {
        UUID userId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        RefreshToken refreshToken = refreshToken(userId, familyId, Instant.parse("2026-04-10T12:00:00Z"));
        refreshToken.setRevokedAt(Instant.parse("2026-04-03T11:00:00Z"));

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(refreshToken));

        assertThrows(UnauthorizedException.class, () -> refreshTokenService.rotate("reused-token"));

        verify(refreshTokenRepository).revokeFamily(eq(familyId), eq(Instant.now(clock)));
        verify(jwtService, never()).generateAccessToken(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void rotate_rejectsDisabledUserAsUnauthorized() {
        UUID userId = UUID.randomUUID();
        RefreshToken refreshToken = refreshToken(userId, UUID.randomUUID(), Instant.parse("2026-04-10T12:00:00Z"));
        User disabledUser = new User();
        disabledUser.setId(userId);
        disabledUser.setEnabled(false);
        disabledUser.setUserType(UserType.MEMBER);

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(refreshToken));
        when(userRepository.findById(userId)).thenReturn(Optional.of(disabledUser));

        assertThrows(UnauthorizedException.class, () -> refreshTokenService.rotate("valid-but-disabled-user"));

        verify(jwtService, never()).generateAccessToken(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void rotate_returnsNewTokensForValidUser() {
        UUID userId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        RefreshToken currentToken = refreshToken(userId, familyId, Instant.parse("2026-04-10T12:00:00Z"));
        User user = new User();
        user.setId(userId);
        user.setEmail("member@example.com");
        user.setName("Member");
        user.setEnabled(true);
        user.setUserType(UserType.MEMBER);

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(currentToken));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(eq(userId), eq("member@example.com"), eq("Member"), any(), eq(true)))
                .thenReturn("new-access-token");

        RotateTokensDTO rotated = refreshTokenService.rotate("valid-token");

        assertEquals("new-access-token", rotated.getNewAccessToken());
        assertNotNull(rotated.getNewRefreshToken());
        assertNotEquals("valid-token", rotated.getNewRefreshToken());
        assertEquals(Instant.parse("2026-04-17T12:00:00Z"), rotated.getRefreshExpiresAt());

        ArgumentCaptor<RefreshToken> savedTokens = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, atLeast(3)).save(savedTokens.capture());
        List<RefreshToken> allSaved = savedTokens.getAllValues();
        assertTrue(allSaved.stream().anyMatch(token -> token.getRevokedAt() != null));
        assertTrue(allSaved.stream().anyMatch(token ->
                familyId.equals(token.getFamilyId())
                        && token.getTokenHash() != null
                        && token.getRevokedAt() == null
                        && token != currentToken
        ));
    }

    @Test
    void addRefreshCookie_usesConfiguredCookieAttributes() {
        refreshTokenService.addRefreshCookie(response, "rotated-token");

        ArgumentCaptor<String> headerValue = ArgumentCaptor.forClass(String.class);
        verify(response).addHeader(eq("Set-Cookie"), headerValue.capture());

        String cookie = headerValue.getValue();
        assertTrue(cookie.contains("refreshToken=rotated-token"));
        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("SameSite=None"));
    }

    private static RefreshToken refreshToken(UUID userId, UUID familyId, Instant expiresAt) {
        User user = new User();
        user.setId(userId);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenId(UUID.randomUUID());
        refreshToken.setUser(user);
        refreshToken.setFamilyId(familyId);
        refreshToken.setTokenHash("stored-hash");
        refreshToken.setIssuedAt(Instant.parse("2026-04-03T12:00:00Z"));
        refreshToken.setExpiresAt(expiresAt);
        return refreshToken;
    }
}
