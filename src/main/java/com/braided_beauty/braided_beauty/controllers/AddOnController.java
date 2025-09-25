package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.addOn.AddOnRequestDTO;
import com.braided_beauty.braided_beauty.dtos.addOn.AddOnResponseDTO;
import com.braided_beauty.braided_beauty.services.AddOnService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/addons")
@AllArgsConstructor
public class AddOnController {
    private final AddOnService addOnService;

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

    @PostMapping
    public ResponseEntity<AddOnResponseDTO> create(@Valid @RequestBody AddOnRequestDTO dto) {
        return ResponseEntity.ok(addOnService.save(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddOnResponseDTO> update(@RequestBody AddOnRequestDTO dto, @Valid @PathVariable UUID id) {
        dto.setId(id); // Trust path param as the source of truth
        return ResponseEntity.ok(addOnService.save(dto));
    }

}
