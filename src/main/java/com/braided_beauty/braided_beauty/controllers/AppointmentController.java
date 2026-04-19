package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.appointment.*;
import com.braided_beauty.braided_beauty.records.AppUserPrincipal;
import com.braided_beauty.braided_beauty.records.BookingConfirmationDTO;
import com.braided_beauty.braided_beauty.records.ConfirmationReceiptDTO;
import com.braided_beauty.braided_beauty.records.FinalPaymentConfirmationDTO;
import com.braided_beauty.braided_beauty.services.AppointmentConfirmationService;
import com.braided_beauty.braided_beauty.services.AppointmentService;
import com.stripe.exception.StripeException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@AllArgsConstructor
@Tag(name = "Appointments", description = "Booking, confirmation, and appointment lookup endpoints")
public class AppointmentController {
    private final AppointmentService appointmentService;
    private final AppointmentConfirmationService appointmentConfirmationService;

    /**
     * Creates an appointment and, when required, starts the Stripe deposit checkout flow.
     */
    @PostMapping("/book")
    @Operation(summary = "Create an appointment booking")
    public ResponseEntity<AppointmentCreateResponseDTO> createAppointment(
            @Valid @RequestBody AppointmentRequestDTO dto, @AuthenticationPrincipal AppUserPrincipal principal) throws StripeException {
        return ResponseEntity.ok(appointmentService.createAppointment(dto, principal));
    }

    @PostMapping("/guest/cancel/{token}")
    @Operation(summary = "Cancel a guest appointment with its cancellation token")
    public ResponseEntity<AppointmentResponseDTO> cancelGuestAppointment(@PathVariable String token,
                                                                         @RequestBody String reason) {
        return ResponseEntity.ok(appointmentService.cancelGuestAppointment(token, reason));
    }

    @DeleteMapping("/holds/{id}")
    @Operation(summary = "Release a pending checkout hold before the deposit is paid")
    public ResponseEntity<Void> releaseCheckoutHold(@PathVariable UUID id) throws StripeException {
        appointmentService.releaseCheckoutHold(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/guest/{token}")
    public ResponseEntity<AppointmentResponseDTO> getGuestAppointment(@PathVariable String token) {
        return ResponseEntity.ok(appointmentService.getGuestAppointmentByToken(token));
    }

    @PatchMapping("/cancel")
    public ResponseEntity<AppointmentResponseDTO> cancelAppointment(@Valid @RequestBody CancelAppointmentDTO dto, @AuthenticationPrincipal AppUserPrincipal principal) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(dto, principal.id()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponseDTO> getAppointmentById(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(id));
    }

    @GetMapping
    public ResponseEntity<List<AppointmentResponseDTO>> getAllAppointmentsByDate(LocalDate date){
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

    @GetMapping("/confirm")
    @Operation(summary = "Return confirmation details for a booking confirmation link")
    public ResponseEntity<?> getBookingConfirmation(@RequestParam UUID id, @RequestParam String token) {
        return ResponseEntity.ok(appointmentConfirmationService.getConfirmationByToken(id, token));
    }

    @GetMapping("/confirm/by-session")
    @Operation(summary = "Return deposit checkout confirmation details by Stripe Checkout session id")
    public ResponseEntity<ConfirmationReceiptDTO> getBookingConfirmationBySession(
            @Parameter(description = "Stripe Checkout session id created for the booking deposit")
            @RequestParam("sessionId") String sessionId
    ) {
        return ResponseEntity.ok(appointmentConfirmationService.getConfirmationBySessionId(sessionId));
    }

    @GetMapping("/final/confirm/by-session")
    @Operation(summary = "Return final payment confirmation details by Stripe Checkout session id")
    public ResponseEntity<ConfirmationReceiptDTO> getFinalPaymentConfirmationBySession(
            @Parameter(description = "Stripe Checkout session id created for the final payment")
            @RequestParam("sessionId") String sessionId
    ) {
        return ResponseEntity.ok(appointmentConfirmationService.getFinalConfirmationBySessionId(sessionId));
    }

    @GetMapping(value = "/confirm/ics", produces = "text/calendar")
    @Operation(summary = "Generate an ICS calendar file from a booking confirmation link")
    public ResponseEntity<String> getIcsByToken(@RequestParam UUID id, @RequestParam String token) {
        String ics = appointmentConfirmationService.buildIcs(id, token);
        return ResponseEntity.ok()
                .header("Content-Type", "text/calendar; charset=utf-8")
                .header("Content-Disposition", "inline; filename=braided-beauty-appointment.ics")
                .body(ics);
    }

    @GetMapping(value = "/confirm/ics/by-session", produces = "text/calendar")
    @Operation(summary = "Generate an ICS calendar file from a Stripe Checkout session id")
    public ResponseEntity<String> getIcsBySession(@RequestParam("sessionId") String sessionId) {
        String ics = appointmentConfirmationService.buildIcs(sessionId);
        return ResponseEntity.ok()
                .header("Content-Type", "text/calendar; charset=utf-8")
                .header("Content-Disposition", "inline; filename=braided-beauty-appointment.ics")
                .body(ics);
    }
}
