package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.serviceCategory.ServiceCategoryCreateDTO;
import com.braided_beauty.braided_beauty.dtos.serviceCategory.ServiceCategoryResponseDTO;
import com.braided_beauty.braided_beauty.dtos.serviceCategory.ServiceCategoryUpdateDTO;
import com.braided_beauty.braided_beauty.services.ServiceCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/category")
@RequiredArgsConstructor
public class AdminServiceCategoryController {
    private final ServiceCategoryService service;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<ServiceCategoryResponseDTO>> getAllCategories(
            @RequestParam(required = false) String search,
            Pageable pageable
    ) {
        try {
            Page<ServiceCategoryResponseDTO> page = service.getAllCategories(search, pageable);
            return ResponseEntity.ok(page);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ServiceCategoryResponseDTO> getCategory(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getCategory(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ServiceCategoryResponseDTO> create(@Valid @RequestBody ServiceCategoryCreateDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<ServiceCategoryResponseDTO> update(@Valid @RequestBody ServiceCategoryUpdateDTO dto, @PathVariable UUID id) {
        return ResponseEntity.ok(service.update(dto, id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
