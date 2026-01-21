package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.appointment.AdminAppointmentRequestDTO;
import com.braided_beauty.braided_beauty.dtos.appointment.AdminAppointmentSummaryDTO;
import com.braided_beauty.braided_beauty.records.CheckoutLinkResponse;
import com.braided_beauty.braided_beauty.services.AdminAppointmentService;
import com.stripe.exception.StripeException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/appointment")
@RequiredArgsConstructor
public class AdminAppointmentController {
    private final AdminAppointmentService adminAppointmentService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/closeout-cash")
    public ResponseEntity<AdminAppointmentSummaryDTO> adminCompleteAppointmentCash(@RequestBody @Valid AdminAppointmentRequestDTO dto) {
        log.info("DTO appointmentId={}", dto.getAppointmentId());
        return ResponseEntity.ok(adminAppointmentService.adminCompleteAppointmentCash(dto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/closeout-stripe")
    public ResponseEntity<CheckoutLinkResponse> adminCompleteAppointmentStripe(@RequestBody AdminAppointmentRequestDTO dto) throws StripeException {
        return ResponseEntity.ok(adminAppointmentService.adminCreateFinalPaymentViaDebitCard(dto.getAppointmentId(), dto.getTipAmount()));
    }


    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all-appointments")
    public ResponseEntity<Page<AdminAppointmentSummaryDTO>> getAllAppointments(Pageable pageable) {
        return ResponseEntity.ok(adminAppointmentService.getAllAppointments(pageable));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<AdminAppointmentSummaryDTO> getAppointmentById(@PathVariable UUID id) {
        return ResponseEntity.ok(adminAppointmentService.getAppointmentById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<AdminAppointmentSummaryDTO> updateAppointment(@PathVariable UUID id, @RequestBody AdminAppointmentRequestDTO dto) throws BadRequestException {
        return ResponseEntity.ok(adminAppointmentService.adminUpdateAppointment(id,dto));
    }

}
