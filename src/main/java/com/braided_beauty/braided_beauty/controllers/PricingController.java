package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.records.BookingPricingPreview;
import com.braided_beauty.braided_beauty.records.BookingPricingPreviewRequest;
import com.braided_beauty.braided_beauty.services.PricingService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pricing")
@AllArgsConstructor
public class PricingController {
    private final PricingService pricingService;

    @PostMapping("/preview")
    public ResponseEntity<BookingPricingPreview> preview(@RequestBody BookingPricingPreviewRequest dto) {
        return ResponseEntity.ok(pricingService.previewBookingPricing(dto.serviceId(), dto.addOnIds(), dto.promoText()));
    }
}
