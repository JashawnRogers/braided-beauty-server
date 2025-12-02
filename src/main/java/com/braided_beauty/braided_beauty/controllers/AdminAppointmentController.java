package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentResponseDTO;
import com.braided_beauty.braided_beauty.services.AppointmentService;
import com.stripe.exception.StripeException;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/appointment")
@RequiredArgsConstructor
public class AdminAppointmentController {
    private final AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<AppointmentResponseDTO> completeAppointment(
            @Parameter(description = "UUID of the appointment to complete", required = true)
            @RequestParam UUID appointmentId) throws StripeException {
        return ResponseEntity.ok(appointmentService.completeAppointment(appointmentId));
    }

}
