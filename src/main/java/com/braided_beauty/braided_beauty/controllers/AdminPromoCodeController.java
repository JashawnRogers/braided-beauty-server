package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.records.PromoCodeDTO;
import com.braided_beauty.braided_beauty.services.AdminPromoCodeService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/promo-codes")
@AllArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPromoCodeController {
    private final AdminPromoCodeService service;

    @GetMapping
    public ResponseEntity<Page<PromoCodeDTO>> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "true") Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(service.listPromoCodes(query, active, page, size));
    }
}
