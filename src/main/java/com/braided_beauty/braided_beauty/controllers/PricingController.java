package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.records.BookingPricingPreview;
import com.braided_beauty.braided_beauty.records.BookingPricingPreviewRequest;
import com.braided_beauty.braided_beauty.services.PricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pricing")
@AllArgsConstructor
@Tag(name = "Pricing", description = "Public pricing preview endpoints")
public class PricingController {
    private final PricingService pricingService;

    /**
     * Calculates booking totals before an appointment is created.
     */
    @PostMapping("/preview")
    @Operation(summary = "Preview service, add-on, and promo pricing for a booking")
    public ResponseEntity<BookingPricingPreview> preview(@RequestBody BookingPricingPreviewRequest dto) {
        return ResponseEntity.ok(pricingService.previewBookingPricing(dto.serviceId(), dto.addOnIds(), dto.promoText()));
    }
}
