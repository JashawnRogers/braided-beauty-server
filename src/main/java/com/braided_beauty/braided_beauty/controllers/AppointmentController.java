package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentRequestDTO;
import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentResponseDTO;
import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentSummaryDTO;
import com.braided_beauty.braided_beauty.dtos.appointment.CancelAppointmentDTO;
import com.braided_beauty.braided_beauty.records.AppUserPrincipal;
import com.braided_beauty.braided_beauty.services.AppointmentService;
import com.stripe.exception.StripeException;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@AllArgsConstructor
public class AppointmentController {
    private final AppointmentService appointmentService;

    @Operation(
            summary = "Create an appointment",
            description = "Creates a new appointment and initiates a deposit payment via Stripe. Returns the appointment details and Stripe session info.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "The appointment details and service options",
                    required = true,
                    content = @Content(schema = @Schema(implementation = AppointmentRequestDTO.class))
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Appointment successfully created", content = @Content(schema = @Schema(implementation = AppointmentResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
            @ApiResponse(responseCode = "500", description = "Stripe or internal server error", content = @Content)
    })

    @PostMapping("/book")
    public ResponseEntity<AppointmentResponseDTO> createAppointment(
            @Valid @RequestBody AppointmentRequestDTO dto) throws StripeException {
        return ResponseEntity.ok(appointmentService.createAppointment(dto));
    }

    @Operation(
            summary = "Mark an appointment as completed",
            description = "Marks the appointment as completed and charges the remaining balance via Stripe."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Appointment successfully completed", content = @Content(schema = @Schema(implementation = AppointmentResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Appointment not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Stripe or internal server error", content = @Content)
    })
    @PostMapping
    public ResponseEntity<AppointmentResponseDTO> completeAppointment(
            @Parameter(description = "UUID of the appointment to complete", required = true)
            @RequestParam UUID appointmentId) throws StripeException {
        return ResponseEntity.ok(appointmentService.completeAppointment(appointmentId));
    }

    @Operation(
            summary = "Cancel an appointment",
            description = "Cancels an existing appointment and updates its status accordingly."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Appointment successfully canceled", content = @Content(schema = @Schema(implementation = AppointmentResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid cancellation request", content = @Content),
            @ApiResponse(responseCode = "404", description = "Appointment not found", content = @Content)
    })
    @PatchMapping("/cancel")
    public ResponseEntity<AppointmentResponseDTO> cancelAppointment(
            @Parameter(description = "DTO containing appointment ID and cancellation reason", required = true)
            @Valid @RequestBody CancelAppointmentDTO dto) throws StripeException{
        return ResponseEntity.ok(appointmentService.cancelAppointment(dto));
    }

    @Operation(
            summary = "Get appointment by ID",
            description = "Retrieves the details of a specific appointment using its UUID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Appointment found", content = @Content(schema = @Schema(implementation = AppointmentResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Appointment not found", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponseDTO> getAppointmentById(
            @Parameter(description = "UUID of the appointment to retrieve", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(id));
    }

    @Operation(
            summary = "Get all appointments for a given date",
            description = "Retrieves a list of all appointments scheduled on the specified date."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Appointments retrieved", content = @Content(schema = @Schema(implementation = AppointmentResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date format", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<AppointmentResponseDTO>> getAllAppointmentsByDate(
            @Parameter(description = "Date for which to retrieve appointments (yyyy-MM-dd)", required = true, example = "2025-08-18")
            @RequestParam("date") @org.springframework.format.annotation.DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date){
        return ResponseEntity.ok(appointmentService.getAllAppointmentsByDate(date));
    }

    @GetMapping("/me/previous")
    public ResponseEntity<Page<AppointmentSummaryDTO>> getPreviousAppointments(
            @AuthenticationPrincipal AppUserPrincipal principal,
            Pageable pageable
            ) {
        return ResponseEntity.ok(appointmentService.getPreviousAppointments(principal.id(), pageable));
    }

    @GetMapping("/me/next")
    public ResponseEntity<AppointmentSummaryDTO> getNextAppointment(@AuthenticationPrincipal AppUserPrincipal principal){
        AppointmentSummaryDTO nextAppointment = appointmentService.getNextAppointment(principal.id()).orElse(null);
        return ResponseEntity.ok(nextAppointment);
    }
}
