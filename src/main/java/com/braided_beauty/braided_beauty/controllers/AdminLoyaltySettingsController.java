package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.loyaltyRecord.LoyaltySettingsDTO;
import com.braided_beauty.braided_beauty.services.LoyaltyService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/loyalty/settings")
@AllArgsConstructor
public class AdminLoyaltySettingsController {
    private final LoyaltyService service;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<LoyaltySettingsDTO> getSettings() {
        return ResponseEntity.ok(service.getSettings());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping
    public ResponseEntity<LoyaltySettingsDTO> updateSettings(
            @Valid @RequestBody LoyaltySettingsDTO dto) {
        return ResponseEntity.ok(service.updateSettings(dto));
    }
}
