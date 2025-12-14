package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.shared.BusinessHoursResponseDTO;
import com.braided_beauty.braided_beauty.services.BusinessHoursService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hours")
@AllArgsConstructor
public class BusinessHoursController {
    private final BusinessHoursService service;

    @GetMapping
    public ResponseEntity<List<BusinessHoursResponseDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BusinessHoursResponseDTO> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getOne(id));
    }

}
