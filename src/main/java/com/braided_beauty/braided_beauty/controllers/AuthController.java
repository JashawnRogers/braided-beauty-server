package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.user.auth.LoginRequestDTO;
import com.braided_beauty.braided_beauty.dtos.user.auth.UserRegistrationDTO;
import com.braided_beauty.braided_beauty.dtos.user.global.ChangePasswordRequestDTO;
import com.braided_beauty.braided_beauty.records.*;
import com.braided_beauty.braided_beauty.services.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


import static com.braided_beauty.braided_beauty.services.RefreshTokenService.COOKIE_NAME;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
@Tag(name = "Auth", description = "Authentication, token rotation, and password recovery")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody @Valid LoginRequestDTO dto, HttpServletResponse res) {
        authService.login(dto, res);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid UserRegistrationDTO dto, HttpServletResponse res) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerAndIssueTokens(dto, res));
    }


    @PostMapping("/refresh")
    @Operation(summary = "Rotate the refresh token cookie and return a fresh access token")
    public ResponseEntity<AccessTokenResponse> refresh(
            @Parameter(description = "Refresh token cookie set during login", required = false)
            @CookieValue(value = COOKIE_NAME, required = false) String cookieToken,
            HttpServletResponse res
    ) {
        AccessTokenResponse out = authService.refresh(cookieToken, res);
        return ResponseEntity.ok(out);
    }


    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(value = COOKIE_NAME, required = false) String cookieToken, HttpServletResponse res) {
      authService.logout(cookieToken, res);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/change-password")
    @Operation(summary = "Change the authenticated user's password")
    public ResponseEntity<Void> updatePassword(@AuthenticationPrincipal AppUserPrincipal principal, @RequestBody ChangePasswordRequestDTO dto) {
        authService.updatePassword(principal.id(), dto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Send a password reset email when the address belongs to an existing user")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestPasswordReset(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Complete a password reset using the emailed reset token")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(
                request.token(),
                request.newPassword(),
                request.confirmPassword()
        );
        return ResponseEntity.noContent().build();
    }

}
