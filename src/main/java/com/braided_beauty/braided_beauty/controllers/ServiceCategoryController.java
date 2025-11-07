package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.serviceCategory.ServiceCategoryCreateDTO;
import com.braided_beauty.braided_beauty.dtos.serviceCategory.ServiceCategoryUpdateDTO;
import com.braided_beauty.braided_beauty.dtos.serviceCategory.ServiceCategoryResponseDTO;
import com.braided_beauty.braided_beauty.services.ServiceCategoryService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @GetMapping("/{id}")
    public ResponseEntity<ServiceCategoryResponseDTO> getCategory(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getCategory(id));
    }

    @GetMapping
    public ResponseEntity<List<ServiceCategoryResponseDTO>> getCategories() {
        return ResponseEntity.ok(service.getAllCategories());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
