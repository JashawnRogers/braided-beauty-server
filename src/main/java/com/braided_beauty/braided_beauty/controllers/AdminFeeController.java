package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.records.CreateFeeDTO;
import com.braided_beauty.braided_beauty.records.FeeRequestDTO;
import com.braided_beauty.braided_beauty.records.FeeResponseDTO;
import com.braided_beauty.braided_beauty.services.FeeService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/fee")
@AllArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFeeController {
    private final FeeService feeService;

    @PostMapping
    public ResponseEntity<FeeResponseDTO> createFee(@RequestBody CreateFeeDTO dto) {
        return ResponseEntity.ok(feeService.createFee(dto));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<FeeResponseDTO> updateFee(@RequestBody FeeRequestDTO dto, @PathVariable UUID id) {
        return ResponseEntity.ok(feeService.updateFee(dto, id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FeeResponseDTO> getFee(@RequestParam UUID id) {
        return ResponseEntity.ok(feeService.getFee(id));
    }

    @GetMapping
    public ResponseEntity<Page<FeeResponseDTO>> listFees(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(feeService.listFees(search, page, size));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@RequestParam UUID id) {
        feeService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
