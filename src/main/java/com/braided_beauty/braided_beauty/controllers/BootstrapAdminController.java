package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.bootstrap.BootstrapAdminRequestDTO;
import com.braided_beauty.braided_beauty.exceptions.UnauthorizedException;
import com.braided_beauty.braided_beauty.records.AppUserPrincipal;
import com.braided_beauty.braided_beauty.records.BootstrapAdminResponse;
import com.braided_beauty.braided_beauty.services.BootstrapAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bootstrap")
@RequiredArgsConstructor
public class BootstrapAdminController {
    private final BootstrapAdminService bootstrapAdminService;

    @PostMapping("/admin")
    public ResponseEntity<BootstrapAdminResponse> bootstrapAdmin(
            Authentication authentication,
            @Valid @RequestBody BootstrapAdminRequestDTO request
    ) {
        UUID userId = extractUserId(authentication);
        return ResponseEntity.ok(bootstrapAdminService.bootstrapCurrentUser(userId, request.getSecret()));
    }

    private UUID extractUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User is not authenticated.");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AppUserPrincipal appUserPrincipal) {
            return appUserPrincipal.id();
        }

        throw new UnauthorizedException("Unsupported authenticated principal.");
    }
}
