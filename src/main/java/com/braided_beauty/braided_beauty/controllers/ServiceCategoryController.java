package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.serviceCategory.ServiceCategoryCreateDTO;
import com.braided_beauty.braided_beauty.dtos.serviceCategory.ServiceCategoryResponseDTO;
import com.braided_beauty.braided_beauty.dtos.serviceCategory.ServiceCategoryUpdateDTO;
import com.braided_beauty.braided_beauty.services.ServiceCategoryService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/category")
@AllArgsConstructor
public class ServiceCategoryController {
    private final ServiceCategoryService service;

    @PostMapping
    public ResponseEntity<ServiceCategoryResponseDTO> create(@Valid @RequestBody ServiceCategoryCreateDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceCategoryResponseDTO> update(@Valid @RequestBody ServiceCategoryUpdateDTO dto, @PathVariable UUID id) {
        return ResponseEntity.ok(service.update(dto, id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceCategoryResponseDTO> getCategory(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getCategory(id));
    }

    @GetMapping
    public ResponseEntity<Page<ServiceCategoryResponseDTO>> getAllAddOns(
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
