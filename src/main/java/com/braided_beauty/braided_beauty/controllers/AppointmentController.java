package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentRequestDTO;
import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentResponseDTO;
import com.braided_beauty.braided_beauty.dtos.appointment.CancelAppointmentDTO;
import com.braided_beauty.braided_beauty.services.AppointmentService;
import com.stripe.exception.StripeException;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<AppointmentResponseDTO> createAppointment(@Valid @RequestBody AppointmentRequestDTO dto) throws StripeException {
        return ResponseEntity.ok(appointmentService.createAppointment(dto));
    }

    @PostMapping
    public ResponseEntity<AppointmentResponseDTO> completeAppointment(UUID appointmentId) throws StripeException {
        return ResponseEntity.ok(appointmentService.completeAppointment(appointmentId));
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
    public ResponseEntity<List<AppointmentResponseDTO>> getAllAppointmentsByDate(@RequestParam("date")
                                                                                     @org.springframework.format.annotation.DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                                                     LocalDate date){
        return ResponseEntity.ok(appointmentService.getAllAppointmentsByDate(date));
    }
}
