package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.records.BookingPricingPreview;
import com.braided_beauty.braided_beauty.services.PricingService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pricing")
@AllArgsConstructor
public class PricingController {
    private final PricingService pricingService;

    @GetMapping("/pricing/preview")
    public ResponseEntity<BookingPricingPreview> preview(
            @RequestParam UUID serviceId,
            @RequestParam List<UUID> addOnIds,
            @RequestParam String pomoText
            ) {
        return ResponseEntity.ok(pricingService.previewBookingPricing(serviceId, addOnIds, pomoText));
    }
}
