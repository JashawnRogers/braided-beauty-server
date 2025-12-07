package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.user.auth.LoginRequestDTO;
import com.braided_beauty.braided_beauty.dtos.user.auth.UserRegistrationDTO;
import com.braided_beauty.braided_beauty.dtos.user.global.ChangePasswordRequestDTO;
import com.braided_beauty.braided_beauty.models.User;
import com.braided_beauty.braided_beauty.records.AppUserPrincipal;
import com.braided_beauty.braided_beauty.services.AuthService;
import com.braided_beauty.braided_beauty.services.JwtService;
import com.braided_beauty.braided_beauty.services.RefreshTokenService;
import com.braided_beauty.braided_beauty.services.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuthService authService;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    private static final String COOKIE_NAME = "refreshToken";
    private static final Duration COOKIE_TTL = Duration.ofDays(14);

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO dto, HttpServletResponse res) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
        );

        User user = userService.findUserByEmailOrThrow(dto.getEmail());

        UUID userId = user.getId();
        String email = user.getEmail();
        String name = user.getName();

        String accessToken = jwtService.generateAccessToken(userId, email, name, auth.getAuthorities());

        var issued = refreshTokenService.issueForUser(userId, "web");
        addRefreshCookie(res, issued.getRefreshToken());

        return ResponseEntity.ok(Map.of("accessToken", accessToken));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRegistrationDTO dto, HttpServletResponse res) {
        User user = authService.register(dto);

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.password)
        );

        UUID userId = user.getId();
        String email = dto.getEmail();
        String name = dto.getName();

        String accessToken = jwtService.generateAccessToken(userId, email, name, auth.getAuthorities());

        var issued = refreshTokenService.issueForUser(userId, "web");
        addRefreshCookie(res, issued.getRefreshToken());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("accessToken", accessToken));
    }


    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(value = COOKIE_NAME, required = false) String cookieToken,
                                     HttpServletResponse res) {
        if (cookieToken == null || cookieToken.isBlank()) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing refresh token"));
        }

        var rotatedToken = refreshTokenService.rotate(cookieToken);
        addRefreshCookie(res, rotatedToken.getNewRefreshToken());
        return ResponseEntity.ok(Map.of("accessToken", rotatedToken.getNewAccessToken()));
    }


    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(value = COOKIE_NAME, required = false) String cookieToken,
                                       HttpServletResponse res) {
        if (cookieToken != null && !cookieToken.isBlank()) {
            refreshTokenService.revoke(cookieToken);
        }

        clearRefreshCookie(res);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/change-password")
    public ResponseEntity<Void> updatePassword(@AuthenticationPrincipal AppUserPrincipal principal, @RequestBody ChangePasswordRequestDTO dto) {
        authService.updatePassword(principal.id(), dto);
        return ResponseEntity.noContent().build();
    }

    private void addRefreshCookie(HttpServletResponse res, String token) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(false)
                .sameSite("None")
                .path("/")
                .maxAge(COOKIE_TTL)
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse res) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("None")
                .path("/")
                .maxAge(0)
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
