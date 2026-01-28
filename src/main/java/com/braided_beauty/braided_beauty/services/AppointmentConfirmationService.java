package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.addOn.AddOnResponseDTO;
import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import com.braided_beauty.braided_beauty.enums.PaymentStatus;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.mappers.addOn.AddOnDTOMapper;
import com.braided_beauty.braided_beauty.models.Appointment;
import com.braided_beauty.braided_beauty.models.Payment;
import com.braided_beauty.braided_beauty.records.BookingConfirmationDTO;
import com.braided_beauty.braided_beauty.records.BookingConfirmationToken;
import com.braided_beauty.braided_beauty.records.ConfirmationReceiptDTO;
import com.braided_beauty.braided_beauty.repositories.AppointmentRepository;
import com.braided_beauty.braided_beauty.repositories.PaymentRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AppointmentConfirmationService {
    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;
    private final AddOnDTOMapper addOnDTOMapper;
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
                appointment.getPaymentStatus(),
                appointment.getRemainingBalance()
                );
    }

    public ConfirmationReceiptDTO getConfirmationBySessionId(String sessionId) {
        Appointment appointment = appointmentRepository.findByStripeSessionId(sessionId)
                .orElseThrow(() -> new NotFoundException("Appointment not found from session: " + sessionId));



        if (appointment.getPaymentStatus() == PaymentStatus.PAYMENT_FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment failed.");
        }

        List<AddOnResponseDTO> addOns = appointment.getAddOns()
                .stream()
                .map(addOnDTOMapper::toDto)
                .toList();

        return new ConfirmationReceiptDTO(
                appointment.getId(),
                appointment.getService().getName(),
                appointment.getService().getPrice(),
                appointment.getAppointmentTime(),
                appointment.getDurationMinutes(),
                appointment.getDepositAmount(),
                appointment.getRemainingBalance(),
                appointment.getTipAmount(),
                addOns,
                appointment.getTotalAmount()
        );
    }

    public ConfirmationReceiptDTO getFinalConfirmationBySessionId(String sessionId) {
        Payment finalPayment = paymentRepository.findByStripeSessionId(sessionId)
                .orElseThrow(() -> new NotFoundException("No payment found with Stripe session ID: " + sessionId));

        Appointment appointment = finalPayment.getAppointment();

        if (appointment.getPaymentStatus() == PaymentStatus.PENDING_PAYMENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Final payment not processed yet.");
        }

        if (appointment.getPaymentStatus() == PaymentStatus.PAYMENT_FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment failed.");
        }

        List<AddOnResponseDTO> addOns = appointment.getAddOns()
                .stream()
                .map(addOnDTOMapper::toDto)
                .toList();

        return new ConfirmationReceiptDTO(
                appointment.getId(),
                appointment.getService().getName(),
                appointment.getService().getPrice(),
                appointment.getAppointmentTime(),
                appointment.getDurationMinutes(),
                appointment.getDepositAmount(),
                appointment.getRemainingBalance(),
                appointment.getTipAmount(),
                addOns,
                appointment.getTotalAmount()
        );
    }

    @Transactional
    public String buildIcs(UUID appointmentId, String token) {
        BookingConfirmationDTO dto = getConfirmationByToken(appointmentId, token);

        Appointment appointment = appointmentRepository.findById(dto.appointmentId())
                .orElseThrow(() -> new NotFoundException("Appointment not found"));

        if (appointment.getBookingConfirmationJti() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Confirmation link is no longer available");
        }

        if (appointment.getAppointmentStatus() != AppointmentStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Deposit for appointment has not been paid");
        }

        ZonedDateTime start = dto.appointmentTime().atZone(ZoneId.of("America/Phoenix"));
        ZonedDateTime end = start.plusMinutes(dto.durationMinutes());

        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
        String dtStartUtc = start.withZoneSameInstant(ZoneOffset.UTC).format(format);
        String dtEndUtc = end.withZoneSameInstant(ZoneOffset.UTC).format(format);

        String uid = dto.appointmentId() + "@braided-beauty";

        return String.join("\r\n",
                "BEGIN:VCALENDAR",
                "VERSION:2.0",
                "PRODID:-//Braided Beauty//Booking//EN",
                "CALSCALE:GREGORIAN",
                "METHOD:PUBLISH",
                "UID:" + uid,
                "DTSTAMP:" + ZonedDateTime.now(ZoneOffset.UTC).format(format),
                "DTSTART:" + dtStartUtc,
                "DTEND:" + dtEndUtc,
                "SUMMARY:" + escapeIcs("Braided Beauty - " + dto.serviceName()),
                "DESCRIPTION:" + escapeIcs("Deposit: $" + dto.depositAmount() + " | Remaining: $" + dto.remainingBalance()),
                "END:VEVENT",
                "END:VCALENDAR",
                ""
                );
    }

    @Transactional
    public String buildIcs(String sessionId) {
        Appointment appointment = appointmentRepository.findByStripeSessionId(sessionId)
                .orElseThrow(() -> new NotFoundException("No appointment found from session: " + sessionId));

        if (appointment.getBookingConfirmationJti() == null) {
           throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Confirmation link is no longer available");
        }

        if (appointment.getAppointmentStatus() != AppointmentStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Deposit for appointment has not been paid");
        }

        ConfirmationReceiptDTO dto = getConfirmationBySessionId(appointment.getStripeSessionId());

        ZonedDateTime start = dto.appointmentTime().atZone(ZoneId.of("America/Phoenix"));
        ZonedDateTime end = start.plusMinutes(dto.durationMinutes());

        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
        String dtStartUtc = start.withZoneSameInstant(ZoneOffset.UTC).format(format);
        String dtEndUtc = end.withZoneSameInstant(ZoneOffset.UTC).format(format);

        String uid = dto.appointmentId() + "@braided-beauty";

        return String.join("\r\n",
                "BEGIN:VCALENDAR",
                "VERSION:2.0",
                "PRODID:-//Braided Beauty//Booking//EN",
                "CALSCALE:GREGORIAN",
                "METHOD:PUBLISH",
                "UID:" + uid,
                "DTSTAMP:" + ZonedDateTime.now(ZoneOffset.UTC).format(format),
                "DTSTART:" + dtStartUtc,
                "DTEND:" + dtEndUtc,
                "SUMMARY:" + escapeIcs("Braided Beauty - " + dto.serviceName()),
                "DESCRIPTION:" + escapeIcs("Deposit: $" + dto.depositAmount() + " | Remaining: $" + dto.remainingBalance()),
                "END:VEVENT",
                "END:VCALENDAR",
                ""
        );
    }

    private String escapeIcs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace(",", "\\,")
                .replace(";", "\\;");
    }
}
