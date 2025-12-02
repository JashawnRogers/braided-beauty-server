package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.service.ServiceCreateDTO;
import com.braided_beauty.braided_beauty.dtos.service.ServiceRequestDTO;
import com.braided_beauty.braided_beauty.dtos.service.ServiceResponseDTO;
import com.braided_beauty.braided_beauty.services.ServicesService;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/api/v1/admin/service")
@RequiredArgsConstructor
public class AdminServiceController {
    private final ServicesService service;


    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<ServiceResponseDTO>> getAllServices(
            @RequestParam(required = false, name = "name") String name,
            @RequestParam(required = false, name = "search") String search,
            Pageable pageable
    ){
        try {
            String needle  = (name != null && !name.isBlank())
                    ? name
                    : (search != null && !search.isBlank() ? search : null);

            Page<ServiceResponseDTO> page = service.getAllServices(needle, pageable);
            return ResponseEntity.ok(page);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{serviceId}")
    public ResponseEntity<ServiceResponseDTO> getServiceById(
            @Parameter(description = "UUID of the service to retrieve", required = true)
            @PathVariable UUID serviceId){
        return ResponseEntity.ok(service.getServiceById(serviceId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ServiceResponseDTO> createService(@Valid @RequestBody ServiceCreateDTO dto){
        ServiceResponseDTO newService = service.createService(dto);
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
    public ResponseEntity<ServiceResponseDTO> updateService(
            @PathVariable UUID serviceId,
            @Valid @RequestBody ServiceRequestDTO dto){
        ServiceResponseDTO updatedService = service.updateService(serviceId, dto);
        return ResponseEntity.ok(updatedService);
    }
}
