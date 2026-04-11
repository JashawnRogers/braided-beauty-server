package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.timeSlots.AvailableTimeSlotsDTO;
import com.braided_beauty.braided_beauty.services.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/availability")
@RequiredArgsConstructor
@Tag(name = "Availability", description = "Public availability lookup for booking")
public class AvailabilityController {
    private final AvailabilityService availabilityService;

    @GetMapping
    @Operation(summary = "List available time slots for a service on a given date")
    public ResponseEntity<List<AvailableTimeSlotsDTO>> getAvailability(
            @Parameter(description = "Service to evaluate availability for")
            @RequestParam UUID serviceId,
            @Parameter(description = "Local business date in ISO-8601 format")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Optional add-on ids included in the availability calculation")
            @RequestParam(required = false) List<UUID> addOnIds) {
        return ResponseEntity.ok(availabilityService.getAvailability(serviceId, date, addOnIds));
    }
}
