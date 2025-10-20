package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.shared.BusinessHoursRequestDTO;
import com.braided_beauty.braided_beauty.dtos.shared.BusinessHoursResponseDTO;
import com.braided_beauty.braided_beauty.services.BusinessHoursService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
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
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping
    public ResponseEntity<BusinessHoursResponseDTO> create(@Valid @RequestBody BusinessHoursRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<BusinessHoursResponseDTO> update(@Valid @RequestBody BusinessHoursRequestDTO dto,
                                                           @PathVariable UUID id) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
