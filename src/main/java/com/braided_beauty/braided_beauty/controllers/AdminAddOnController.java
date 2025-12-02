package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.addOn.AddOnRequestDTO;
import com.braided_beauty.braided_beauty.dtos.addOn.AddOnResponseDTO;
import com.braided_beauty.braided_beauty.services.AddOnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/addons")
@RequiredArgsConstructor
public class AdminAddOnController {
    private final AddOnService addOnService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<AddOnResponseDTO> getAddOn(@PathVariable UUID id) {
        return ResponseEntity.ok(addOnService.getAddOn(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<AddOnResponseDTO>> getAllAddOns(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAtFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAtTo,
            Pageable pageable
    ){
        try {
            Page<AddOnResponseDTO> page = addOnService.getAllAddOns(search, createdAtFrom,createdAtTo, pageable);
            return ResponseEntity.ok(page);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }


    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<AddOnResponseDTO> create(@Valid @RequestBody AddOnRequestDTO dto) {
        return ResponseEntity.ok(addOnService.save(dto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<AddOnResponseDTO> update(@Valid @RequestBody AddOnRequestDTO dto, @PathVariable UUID id) {
        dto.setId(id); // Trust path param as the source of truth
        return ResponseEntity.ok(addOnService.save(dto));
    }
}
