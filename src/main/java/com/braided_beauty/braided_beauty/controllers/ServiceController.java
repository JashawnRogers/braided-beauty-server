package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.service.ServiceResponseDTO;
import com.braided_beauty.braided_beauty.services.ServicesService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/service")
@AllArgsConstructor
public class ServiceController {
    private final ServicesService service;

    @GetMapping
    public ResponseEntity<List<ServiceResponseDTO>> getAllServicesByList() {
        return ResponseEntity.ok(service.getAllServicesByList());
    }

    @GetMapping("/{serviceId}")
    public ResponseEntity<ServiceResponseDTO> getServiceById(
            @Parameter(description = "UUID of the service to retrieve", required = true)
            @PathVariable UUID serviceId){
        return ResponseEntity.ok(service.getServiceById(serviceId));
    }
}
