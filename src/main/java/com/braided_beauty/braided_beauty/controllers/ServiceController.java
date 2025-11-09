package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.service.ServiceCreateDTO;
import com.braided_beauty.braided_beauty.dtos.service.ServiceRequestDTO;
import com.braided_beauty.braided_beauty.dtos.service.ServiceResponseDTO;
import com.braided_beauty.braided_beauty.services.ServicesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Operation(
            summary = "Create a new service",
            description = "Allows admins to create a new hairstyling service with title, description, image, and pricing."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Service successfully created",
                    content = @Content(schema = @Schema(implementation = ServiceResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content)
    })
//    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ServiceResponseDTO> createService(@Valid @RequestBody ServiceCreateDTO dto){
        ServiceResponseDTO newService = service.createService(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(newService);
    }

    @Operation(
            summary = "Delete a service",
            description = "Allows admins to delete an existing service by its UUID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service successfully deleted", content = @Content),
            @ApiResponse(responseCode = "404", description = "Service not found", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content)
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{serviceId}")
    public ResponseEntity<String> deleteService(
            @Parameter(description = "UUID of the service to delete", required = true)
            @PathVariable UUID serviceId){
        service.deleteServiceById(serviceId);
        return ResponseEntity.ok("Deleted service with ID: " + serviceId);
    }

    @Operation(
            summary = "Update a service",
            description = "Allows admins to update service details such as description, price, or image."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service successfully updated",
                    content = @Content(schema = @Schema(implementation = ServiceResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Service not found", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content)
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{serviceId}")
    public ResponseEntity<ServiceResponseDTO> updateService(
            @Parameter(description = "UUID of the service to update", required = true)
            @PathVariable UUID serviceId,
            @Valid @RequestBody ServiceRequestDTO dto){
       ServiceResponseDTO updatedService = service.updateService(serviceId, dto);
        return ResponseEntity.ok(updatedService);
    }

    @Operation(
            summary = "Get all services",
            description = "Retrieves a list of all available hairstyling services."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of services returned",
                    content = @Content(schema = @Schema(implementation = ServiceResponseDTO.class)))
    })
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


    @Operation(
            summary = "Get service by ID",
            description = "Retrieves a specific service using its UUID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service found",
                    content = @Content(schema = @Schema(implementation = ServiceResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Service not found", content = @Content)
    })
    @GetMapping("/{serviceId}")
    public ResponseEntity<ServiceResponseDTO> getServiceById(
            @Parameter(description = "UUID of the service to retrieve", required = true)
            @PathVariable UUID serviceId){
        return ResponseEntity.ok(service.getServiceById(serviceId));
    }
}
