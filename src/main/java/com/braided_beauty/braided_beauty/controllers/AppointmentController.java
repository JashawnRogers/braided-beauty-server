package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentRequestDTO;
import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentResponseDTO;
import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentSummaryDTO;
import com.braided_beauty.braided_beauty.dtos.appointment.CancelAppointmentDTO;
import com.braided_beauty.braided_beauty.records.AppUserPrincipal;
import com.braided_beauty.braided_beauty.records.CheckoutLinkResponse;
import com.braided_beauty.braided_beauty.services.AppointmentService;
import com.stripe.exception.StripeException;
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


    @PostMapping("/book")
    public ResponseEntity<CheckoutLinkResponse> createAppointment(
            @Valid @RequestBody AppointmentRequestDTO dto, @AuthenticationPrincipal AppUserPrincipal principal) throws StripeException {
        return ResponseEntity.ok(appointmentService.createAppointment(dto, principal));
    }

    @PostMapping("/guest/cancel")
    public ResponseEntity<AppointmentResponseDTO> cancelGuestAppointment(@RequestParam("token") String token,
                                                                         @RequestParam(value = "reason", required = false) String reason) {
        return ResponseEntity.ok(appointmentService.cancelGuestAppointment(token, reason));
    }

    @PatchMapping("/cancel")
    public ResponseEntity<AppointmentResponseDTO> cancelAppointment(@Valid @RequestBody CancelAppointmentDTO dto) throws StripeException{
        return ResponseEntity.ok(appointmentService.cancelAppointment(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponseDTO> getAppointmentById(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(id));
    }

    @GetMapping
    public ResponseEntity<List<AppointmentResponseDTO>> getAllAppointmentsByDate(
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
