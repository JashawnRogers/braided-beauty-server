package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.records.BusinessSettingsDTO;
import com.braided_beauty.braided_beauty.services.BusinessSettingsService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/business/settings")
@AllArgsConstructor
public class BusinessSettingsController {
    private final BusinessSettingsService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BusinessSettingsDTO> getSettings() {
        return ResponseEntity.ok(service.getSettings());
    }

    @PatchMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BusinessSettingsDTO> updateSettings(@Valid @RequestBody BusinessSettingsDTO dto) {
        return ResponseEntity.ok(service.updateSettings(dto));
    }
}
