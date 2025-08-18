package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.service.ServicePatchDTO;
import com.braided_beauty.braided_beauty.dtos.service.ServiceRequestDTO;
import com.braided_beauty.braided_beauty.dtos.service.ServiceResponseDTO;
import com.braided_beauty.braided_beauty.services.ServicesService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/service")
@AllArgsConstructor
public class ServiceController {
    private final ServicesService service;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ServiceResponseDTO> createService(@Valid @RequestBody ServiceRequestDTO serviceRequestDTO){
        ServiceResponseDTO newService = service.createService(serviceRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(newService);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{serviceId}")
    public ResponseEntity<String> deleteService(@PathVariable UUID serviceId){
        service.deleteServiceById(serviceId);
        return ResponseEntity.ok("Deleted service with ID: " + serviceId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{serviceId}")
    public ResponseEntity<ServiceResponseDTO> updateService(@PathVariable UUID serviceId,@Valid @RequestBody ServicePatchDTO servicePatchDTO){
       ServiceResponseDTO updatedService = service.updateService(serviceId, servicePatchDTO);
        return ResponseEntity.ok(updatedService);
    }

    @GetMapping
    public ResponseEntity<List<ServiceResponseDTO>> getAllServices(){
        return ResponseEntity.ok(service.getAllServices());
    }

    @GetMapping("/{serviceId}")
    public ResponseEntity<ServiceResponseDTO> getServiceById(@PathVariable UUID serviceId){
        return ResponseEntity.ok(service.getServiceById(serviceId));
    }
}
