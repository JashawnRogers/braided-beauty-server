package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentRequestDTO;
import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentResponseDTO;
import com.braided_beauty.braided_beauty.services.AppointmentService;
import com.stripe.exception.StripeException;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/appointments")
@AllArgsConstructor
public class AppointmentController {
    private final AppointmentService appointmentService;

    @PostMapping("/book")
    public ResponseEntity<AppointmentResponseDTO> createAppointment(@RequestBody AppointmentRequestDTO dto) {
        try{
            AppointmentResponseDTO response = appointmentService.createAppointment(dto);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            return ResponseEntity.status(500).build();
        }
    }

}
