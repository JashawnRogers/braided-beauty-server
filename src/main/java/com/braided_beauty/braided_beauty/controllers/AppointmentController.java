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
        AppointmentResponseDTO response = appointmentService.createAppointment(dto);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/cancel")
    public ResponseEntity<AppointmentResponseDTO> cancelAppointment(@Valid @RequestBody CancelAppointmentDTO dto) throws StripeException{
        AppointmentResponseDTO appointment = appointmentService.cancelAppointment(dto);
        return ResponseEntity.ok(appointment);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponseDTO> getAppointmentById(@PathVariable UUID id) {
        AppointmentResponseDTO appointment = appointmentService.getAppointmentById(id);
        return ResponseEntity.ok(appointment);
    }

    @GetMapping
    public ResponseEntity<List<AppointmentResponseDTO>> getAllAppointmentsByDate(@RequestParam("date")
                                                                                     @org.springframework.format.annotation.DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                                                     LocalDate date){
        List<AppointmentResponseDTO> appointments = appointmentService.getAllAppointments(date);
        return ResponseEntity.ok(appointments);
    }
}
