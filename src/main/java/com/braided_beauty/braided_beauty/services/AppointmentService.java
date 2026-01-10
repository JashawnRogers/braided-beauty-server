package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.config.SchedulingConfig;
import com.braided_beauty.braided_beauty.dtos.appointment.*;
import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import com.braided_beauty.braided_beauty.enums.PaymentStatus;
import com.braided_beauty.braided_beauty.exceptions.ConflictException;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.exceptions.UnauthorizedException;
import com.braided_beauty.braided_beauty.mappers.appointment.AppointmentDtoMapper;
import com.braided_beauty.braided_beauty.models.AddOn;
import com.braided_beauty.braided_beauty.models.Appointment;
import com.braided_beauty.braided_beauty.models.ServiceModel;
import com.braided_beauty.braided_beauty.models.User;
import com.braided_beauty.braided_beauty.records.AppUserPrincipal;
import com.braided_beauty.braided_beauty.records.CheckoutLinkResponse;
import com.braided_beauty.braided_beauty.records.FrontendProps;
import com.braided_beauty.braided_beauty.repositories.AppointmentRepository;
import com.braided_beauty.braided_beauty.repositories.ServiceRepository;
import com.braided_beauty.braided_beauty.repositories.UserRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Payment lifecycle for an appointment.
 * Notes:
 * - PENDING_PAYMENT: payment has been initiated (e.g., appointment created / checkout session created) but not yet confirmed by webhook.
 * - PAID_DEPOSIT: deposit payment succeeded.
 * - COMPLETED: paid-in-full (final payment succeeded).
 * - FAILED: a payment attempt failed.
 * - REFUNDED: refunded (full refund for the relevant payment scope).
 * - NO_DEPOSIT_REQUIRED: deposit amount was 0; appointment can proceed without deposit.
 */

@Service
@AllArgsConstructor
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final AppointmentDtoMapper appointmentDtoMapper;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final AddOnService addOnService;
    private final PaymentService paymentService;
    private final SchedulingConfig schedulingConfig;
    private final static Logger log = LoggerFactory.getLogger(AppointmentService.class);
    private final FrontendProps frontendProps;

    @Transactional
    public AppointmentCreateResponseDTO createAppointment(AppointmentRequestDTO dto, AppUserPrincipal principal) throws StripeException {
        ServiceModel service = serviceRepository.findById(dto.getServiceId())
                .orElseThrow(() -> new NotFoundException("Service not found"));

        appointmentRepository.lockSchedule();

        int bufferMinutes = schedulingConfig.getBufferMinutes();
        LocalDateTime start = dto.getAppointmentTime();

        Appointment appointment = appointmentDtoMapper.toEntity(dto);

        // ----- user vs guest -----
        if (principal != null) {
            User user = userRepository.findById(principal.id())
                    .orElseThrow(() -> new NotFoundException("No user found with ID: " + principal.id()));
            appointment.setUser(user);
            appointment.setGuestEmail(null);
        } else {
            String email = dto.getReceiptEmail();
            if (email == null || email.isBlank()) throw new IllegalArgumentException("Guest booking requires a receipt email");
            appointment.setUser(null);
            appointment.setGuestEmail(email);
            appointment.setGuestCancelToken(UUID.randomUUID().toString());
        }

        boolean hasUser = appointment.getUser() != null;
        boolean hasGuestEmail = appointment.getGuestEmail() != null && !appointment.getGuestEmail().isBlank();
        if (hasUser == hasGuestEmail) throw new IllegalStateException("Must have exactly one: user or guest email");

        // ----- service + add-ons -----
        appointment.setService(service);

        List<AddOn> addOns = List.of();
        if (dto.getAddOnIds() != null && !dto.getAddOnIds().isEmpty()) {
            addOns = addOnService.getAddOnIds(dto.getAddOnIds());
            appointment.setAddOns(addOns);
        } else {
            appointment.setAddOns(List.of());
        }

        int addOnMinutes = addOns.stream()
                .map(AddOn::getDurationMinutes)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        BigDecimal addOnsTotal = addOns.stream()
                .map(AddOn::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int durationMinutes = (service.getDurationMinutes() == null ? 0 : service.getDurationMinutes()) + addOnMinutes;

        appointment.setDurationMinutes(durationMinutes);

        // ----- conflict check uses duration + buffer -----
        LocalDateTime end = start.plusMinutes(durationMinutes + bufferMinutes);

        List<Appointment> conflicts = appointmentRepository.findConflictingAppointments(start, end, bufferMinutes);
        if (!conflicts.isEmpty()) throw new ConflictException("This time overlaps with another appointment.");

        // ----- status/payment -----
        BigDecimal total = addOnsTotal
                .add(service.getPrice())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal deposit = total
                .multiply(new BigDecimal("0.20"))
                .setScale(2, RoundingMode.HALF_UP);

        appointment.setDepositAmount(deposit);

        if (deposit.compareTo(BigDecimal.ZERO) <= 0) {
            appointment.setAppointmentStatus(AppointmentStatus.CONFIRMED);
            appointment.setPaymentStatus(PaymentStatus.NO_DEPOSIT_REQUIRED);
            appointment.setHoldExpiresAt(null);
            appointment.setCreatedAt(LocalDateTime.now());
            Appointment savedNoDepositRequired = appointmentRepository.save(appointment);
            return new AppointmentCreateResponseDTO(savedNoDepositRequired.getId(), false, null);
        } else {
            appointment.setAppointmentStatus(AppointmentStatus.PENDING_CONFIRMATION);
            appointment.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);
            appointment.setHoldExpiresAt(LocalDateTime.now().plusMinutes(15));
        }

        appointment.setCreatedAt(LocalDateTime.now());

        Appointment saved = appointmentRepository.save(appointment);

        // ----- Call payment service after save -----
        String successUrl = frontendProps.baseUrl() + "/book/success?session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl = frontendProps.baseUrl() + "/book/cancel?appointmentId=" + saved.getId();
        Session session = paymentService.createDepositCheckoutSession(saved, successUrl, cancelUrl);

        saved.setStripeSessionId(session.getId());
        appointmentRepository.save(saved);

        return new AppointmentCreateResponseDTO(saved.getId(),true ,session.getUrl());
    }

    @Transactional
    public AppointmentResponseDTO cancelAppointment(CancelAppointmentDTO dto) {
        UUID appointmentId = dto.getAppointmentId();
        UUID userId = dto.getUserId();
        String reason = dto.getCancelReason();

        Appointment canceledAppointment = appointmentRepository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new NotFoundException("No appointment found"));

        if (canceledAppointment.getUser() == null) {
            throw new UnauthorizedException("Guest appointments cannot be canceled via this endpoint");
        }

        if  (!canceledAppointment.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You cannot cancel someone else's appointment");
        }

        if (canceledAppointment.getAppointmentStatus() == AppointmentStatus.CANCELED){
            throw new ConflictException("Appointment already canceled");
        }

        canceledAppointment.setAppointmentStatus(AppointmentStatus.CANCELED);
        canceledAppointment.setCancelReason(reason);
        canceledAppointment.setUpdatedAt(LocalDateTime.now());

        appointmentRepository.save(canceledAppointment);

        return appointmentDtoMapper.toDTO(canceledAppointment);
    }

    @Transactional
    public AppointmentResponseDTO cancelGuestAppointment(String token, String reason) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Missing cancel token");
        }

        Appointment appointment = appointmentRepository.findByGuestCancelTokenForUpdate(token)
                .orElseThrow(() -> new NotFoundException("No appointment found for this cancel token"));

        if (appointment.getUser() != null) {
            throw new UnauthorizedException("This cancel token is not valid for member appointments");
        }

        if (appointment.getAppointmentStatus() == AppointmentStatus.CANCELED) {
            throw new ConflictException("Appointment already cancelled");
        }

        appointment.setAppointmentStatus(AppointmentStatus.CANCELED);
        appointment.setCancelReason(reason);
        appointment.setUpdatedAt(LocalDateTime.now());

        appointment.setGuestCancelToken(null);

        appointmentRepository.save(appointment);
        return appointmentDtoMapper.toDTO(appointment);
    }

    public AppointmentResponseDTO getAppointmentById(UUID appointmentId){
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found"));
        log.info("Retrieved service with ID: {}", appointment.getId());
        return appointmentDtoMapper.toDTO(appointment);
    }

    public List<AppointmentResponseDTO> getAllAppointmentsByDate(LocalDate date){
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<Appointment> appointments = appointmentRepository
                .findAllByAppointmentTimeBetweenOrderByAppointmentTimeAsc(start, end);

        return appointments.stream()
                .map(appointmentDtoMapper::toDTO)
                .toList();
    }

    public Optional<AppointmentSummaryDTO> getNextAppointment(UUID userId) {
        LocalDateTime now = LocalDateTime.now();

        List<AppointmentStatus> statuses = List.of(
                AppointmentStatus.COMPLETED
        );

        return appointmentRepository.findFirstByUserIdAndAppointmentTimeAfterAndAppointmentStatusInOrderByAppointmentTimeAsc(userId, now, statuses)
                .map(appointmentDtoMapper::toSummaryDTO);
    }

    public Page<AppointmentSummaryDTO> getPreviousAppointments(UUID userId, Pageable pageable){
        LocalDateTime now = LocalDateTime.now();

        List<AppointmentStatus> statuses = List.of(
                AppointmentStatus.COMPLETED,
                AppointmentStatus.CANCELED
        );

        return appointmentRepository
                .findUserByIdAndAppointmentTimeBeforeAndAppointmentStatusIn(userId, now, statuses, pageable)
                .map(appointmentDtoMapper::toSummaryDTO);
    }
}
