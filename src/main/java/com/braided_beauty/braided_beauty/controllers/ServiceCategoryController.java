package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.serviceCategory.ServiceCategoryResponseDTO;
import com.braided_beauty.braided_beauty.services.ServiceCategoryService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/category")
@AllArgsConstructor
public class ServiceCategoryController {
    private final ServiceCategoryService service;


    @GetMapping
    public ResponseEntity<List<ServiceCategoryResponseDTO>> getAllCategoriesByList() {
        return ResponseEntity.ok(service.getAllCategoriesByList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceCategoryResponseDTO> getCategory(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getCategory(id));
    }
}
