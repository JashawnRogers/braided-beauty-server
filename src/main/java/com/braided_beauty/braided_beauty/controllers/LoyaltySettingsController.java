package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.loyaltyRecord.LoyaltySettingsDTO;
import com.braided_beauty.braided_beauty.services.LoyaltyService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/loyalty/settings")
@AllArgsConstructor
public class LoyaltySettingsController {
    private final LoyaltyService service;

    @GetMapping
    public ResponseEntity<LoyaltySettingsDTO> getSettings() {
        return ResponseEntity.ok(service.getSettings());
    }

    @PutMapping
    public ResponseEntity<LoyaltySettingsDTO> updateSettings(
            @Valid @RequestBody LoyaltySettingsDTO dto) {
        return ResponseEntity.ok(service.updateSettings(dto));
    }
}
