package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.records.PromoCodeDTO;
import com.braided_beauty.braided_beauty.services.AdminPromoCodeService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/promo")
@AllArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPromoCodeController {
    private final AdminPromoCodeService service;

    @GetMapping
    public ResponseEntity<Page<PromoCodeDTO>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "true") Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(service.listPromoCodes(search, active, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromoCodeDTO> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getPromoCode(id));
    }

    @PostMapping
    public ResponseEntity<PromoCodeDTO> create(@RequestBody PromoCodeDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createPromoCode(dto));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PromoCodeDTO> update(@RequestBody PromoCodeDTO dto, @PathVariable UUID id) {
        return ResponseEntity.ok(service.updatePromoCode(dto, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable UUID id) {
        service.softDelete(id);
        return ResponseEntity.noContent().build();
    }

}
