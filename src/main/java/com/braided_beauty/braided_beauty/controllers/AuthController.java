package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.user.auth.LoginRequestDTO;
import com.braided_beauty.braided_beauty.dtos.user.auth.UserRegistrationDTO;
import com.braided_beauty.braided_beauty.dtos.user.global.ChangePasswordRequestDTO;
import com.braided_beauty.braided_beauty.records.AccessTokenResponse;
import com.braided_beauty.braided_beauty.records.AppUserPrincipal;
import com.braided_beauty.braided_beauty.records.AuthResponse;
import com.braided_beauty.braided_beauty.services.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


import static com.braided_beauty.braided_beauty.services.RefreshTokenService.COOKIE_NAME;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;

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
    public ResponseEntity<AccessTokenResponse> refresh(
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
    public ResponseEntity<Void> updatePassword(@AuthenticationPrincipal AppUserPrincipal principal, @RequestBody ChangePasswordRequestDTO dto) {
        authService.updatePassword(principal.id(), dto);
        return ResponseEntity.noContent().build();
    }




}
