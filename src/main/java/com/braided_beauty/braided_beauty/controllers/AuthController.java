package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.user.auth.LoginRequestDTO;
import com.braided_beauty.braided_beauty.dtos.user.auth.UserRegistrationDTO;
import com.braided_beauty.braided_beauty.models.User;
import com.braided_beauty.braided_beauty.services.AuthService;
import com.braided_beauty.braided_beauty.services.JwtService;
import com.braided_beauty.braided_beauty.services.RefreshTokenService;
import com.braided_beauty.braided_beauty.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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


    @Operation(
            summary = "Login with email and password",
            description = "Authenticates user and returns a new JWT access token and sets a refresh token as an HTTP-only cookie.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Login credentials",
                    required = true,
                    content = @Content(schema = @Schema(implementation = LoginRequestDTO.class))
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful. Access token returned."),
            @ApiResponse(responseCode = "401", description = "Invalid email or password", content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO dto, HttpServletResponse res) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
        );

        User user = userService.findUserByEmailOrThrow(dto.getEmail());

        UUID userId = user.getId();
        String email = user.getEmail();
        String name = user.getName();

        String accesToken = jwtService.generateAccessToken(userId, email, name, auth.getAuthorities());

        var issued = refreshTokenService.issueForUser(userId, "web");
        addRefreshCookie(res, issued.getRefreshToken());

        return ResponseEntity.ok(Map.of("accessToken", accesToken));
    }

    @Operation(
            summary = "Register a new user",
            description = "Registers a new user and immediately returns access and refresh tokens.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Registration form data",
                    content = @Content(schema = @Schema(implementation = UserRegistrationDTO.class))
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User successfully registered"),
            @ApiResponse(responseCode = "400", description = "Validation error or duplicate email", content = @Content)
    })
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


    @Operation(
            summary = "Refresh access token",
            description = "Rotates the refresh token and returns a new access token. Refresh token is expected in the HTTP-only cookie."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Access token refreshed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid refresh token", content = @Content)
    })
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

    @Operation(
            summary = "Logout user",
            description = "Revokes the refresh token and clears the HTTP-only refresh cookie."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logout successful"),
            @ApiResponse(responseCode = "200", description = "No refresh token found — no action taken")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(value = COOKIE_NAME, required = false) String cookieToken,
                                       HttpServletResponse res) {
        if (cookieToken != null && !cookieToken.isBlank()) {
            refreshTokenService.revoke(cookieToken);
        }

        clearRefreshCookie(res);
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
