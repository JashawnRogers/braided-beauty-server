package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.enums.PaymentStatus;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.models.Appointment;
import com.braided_beauty.braided_beauty.records.BookingConfirmationDTO;
import com.braided_beauty.braided_beauty.records.BookingConfirmationToken;
import com.braided_beauty.braided_beauty.repositories.AppointmentRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AppointmentConfirmationService {
    private final AppointmentRepository appointmentRepository;
    private final JwtService jwtService;
    private final JwtDecoder jwtDecoder;

    @Transactional
    public BookingConfirmationToken ensureConfirmationTokenForAppointment(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found"));

        if (appointment.getBookingConfirmationJti() != null
                && appointment.getBookingConfirmationExpiresAt() != null &&
                Instant.now().isBefore(appointment.getBookingConfirmationExpiresAt())
        ) {
            return null;
        }

        BookingConfirmationToken confirmationToken = jwtService.generateBookingConfirmationToken(appointment.getId());
        appointment.setBookingConfirmationJti(confirmationToken.jti());
        appointment.setBookingConfirmationExpiresAt(confirmationToken.expiresAt());

        appointmentRepository.save(appointment);
        return confirmationToken;
    }

    @Transactional
    public BookingConfirmationDTO getConfirmationByToken(UUID appointmentId, String token) {
        Jwt decoded = jwtDecoder.decode(token);

        String typ = decoded.getClaimAsString("typ");
        if (!"booking_confirmation".equals(typ)
            || !appointmentId.toString().equals(decoded.getSubject())
        ) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid confirmation token");
        }



        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found."));


        Instant expiresAt = decoded.getExpiresAt();
        if (expiresAt == null || Instant.now().isAfter(expiresAt)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Confirmation link expired");
        }

        String tokenJti = decoded.getId();
        String entityJti = appointment.getBookingConfirmationJti();
        if (tokenJti == null || entityJti == null || !tokenJti.equals(entityJti)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Confirmation link is no longer valid");
        }

        if (appointment.getPaymentStatus() == PaymentStatus.PENDING_PAYMENT) {
            throw new ResponseStatusException(HttpStatus.ACCEPTED, "Payment processing");
        }

        if (appointment.getPaymentStatus() == PaymentStatus.PAYMENT_FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment failed");
        }

        return new BookingConfirmationDTO(
                appointment.getId(),
                appointment.getService().getName(),
                appointment.getAppointmentTime(),
                appointment.getDurationMinutes(),
                appointment.getDepositAmount(),
                appointment.getRemainingBalance(),
                appointment.getPaymentStatus()
                );
    }

    @Transactional
    public BookingConfirmationDTO getConfirmationBySessionId(String sessionId) {
        Appointment appointment = appointmentRepository.findByStripeSessionId(sessionId)
                .orElseThrow(() -> new NotFoundException("Appointment not found from session: " + sessionId));

        if (appointment.getPaymentStatus() == PaymentStatus.PENDING_PAYMENT
            || appointment.getPaymentStatus() == PaymentStatus.PAYMENT_FAILED || appointment.getBookingConfirmationJti() == null) {
            throw new ResponseStatusException(HttpStatus.ACCEPTED, "Payment processing");
        }

        return new BookingConfirmationDTO(
                appointment.getId(),
                appointment.getService().getName(),
                appointment.getAppointmentTime(),
                appointment.getDurationMinutes(),
                appointment.getDepositAmount(),
                appointment.getRemainingBalance(),
                appointment.getPaymentStatus()
        );
    }
}
