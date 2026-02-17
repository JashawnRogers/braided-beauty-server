package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.appointment.*;
import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import com.braided_beauty.braided_beauty.enums.PaymentStatus;
import com.braided_beauty.braided_beauty.enums.UserType;
import com.braided_beauty.braided_beauty.exceptions.BadRequestException;
import com.braided_beauty.braided_beauty.exceptions.ConflictException;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.exceptions.UnauthorizedException;
import com.braided_beauty.braided_beauty.mappers.appointment.AppointmentDtoMapper;
import com.braided_beauty.braided_beauty.models.*;
import com.braided_beauty.braided_beauty.records.AppUserPrincipal;
import com.braided_beauty.braided_beauty.records.BookingConfirmationToken;
import com.braided_beauty.braided_beauty.records.FrontendProps;
import com.braided_beauty.braided_beauty.records.PricingBreakdown;
import com.braided_beauty.braided_beauty.repositories.AppointmentRepository;
import com.braided_beauty.braided_beauty.repositories.ServiceRepository;
import com.braided_beauty.braided_beauty.repositories.UserRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
    private final AppointmentConfirmationService appointmentConfirmationService;
    private final BusinessSettingsService businessSettingsService;
    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;
    private final PricingService pricingService;
    private final PromoCodeValidationService promoCodeValidationService;
    private final static Logger log = LoggerFactory.getLogger(AppointmentService.class);
    private final FrontendProps frontendProps;

    @Transactional
    public AppointmentCreateResponseDTO createAppointment(
            AppointmentRequestDTO dto,
            AppUserPrincipal principal
    ) throws StripeException {

        ServiceModel service = serviceRepository.findById(dto.getServiceId())
                .orElseThrow(() -> new NotFoundException("Service not found"));

        Appointment appointment = appointmentDtoMapper.toEntity(dto);

        // user vs guest
        attachUserOrGuest(appointment, dto, principal);

        // service + add-ons
        attachServiceAndAddOns(appointment, service, dto);

        appointmentRepository.lockSchedule();

        // conflict check
        assertNoConflicts(appointment, businessSettingsService.getAppointmentBufferMinutes());

        // promo code
        PromoCode promoCode = promoCodeValidationService.validateAndGetOrNull(dto.getPromoText());
        if (promoCode != null) {
            appointment.setPromoCode(promoCode);
            appointment.setPromoCodeText(promoCode.getCode());
        }

        // pricing baseline
        PricingBreakdown pricingBreakdown = pricingService.calculate(appointment);
        appointment.setDepositAmount(pricingBreakdown.deposit());
        appointment.setRemainingBalance(pricingBreakdown.postDepositBalance());
        appointment.setDiscountAmount(pricingBreakdown.promoDiscount());
        appointment.setTotalAmount(pricingBreakdown.subtotal());

        // save appointment row (+ create deposit session if needed)
        try {
            if (pricingBreakdown.deposit().compareTo(BigDecimal.ZERO) <= 0) {
                appointment.setAppointmentStatus(AppointmentStatus.CONFIRMED);
                appointment.setPaymentStatus(PaymentStatus.NO_DEPOSIT_REQUIRED);
                appointment.setHoldExpiresAt(null);

                Appointment savedNoDepositRequired = appointmentRepository.saveAndFlush(appointment);

                BookingConfirmationToken confirmationToken =
                        appointmentConfirmationService.ensureConfirmationTokenForAppointment(savedNoDepositRequired.getId());

                sendNoDepositBookingEmailsAfterCommit(savedNoDepositRequired);

                return new AppointmentCreateResponseDTO(
                        savedNoDepositRequired.getId(),
                        false,
                        null,
                        confirmationToken.token()
                );
            }

            // Deposit required
            appointment.setAppointmentStatus(AppointmentStatus.PENDING_CONFIRMATION);
            appointment.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);
            appointment.setHoldExpiresAt(LocalDateTime.now().plusMinutes(15));

            Appointment saved = appointmentRepository.saveAndFlush(appointment);

            String successUrl = frontendProps.baseUrl() + "/book/success?session_id={CHECKOUT_SESSION_ID}";
            String cancelUrl = frontendProps.baseUrl() + "/book/cancel?appointmentId=" + saved.getId();

            Session session = paymentService.createDepositCheckoutSession(saved, successUrl, cancelUrl);

            saved.setStripeSessionId(session.getId());
            appointmentRepository.save(saved);

            return new AppointmentCreateResponseDTO(saved.getId(), true, session.getUrl(), null);

        } catch (DataIntegrityViolationException ex) {
            if (isAppointmentTimeUniqueViolation(ex)) {
                throw new ConflictException("That time was just booked. Please pick another time.");
            }
            throw ex;
        }
    }

    @Transactional
    public AppointmentResponseDTO cancelAppointment(CancelAppointmentDTO dto, UUID userId) {
        Appointment appointment = appointmentRepository.findByIdForUpdate(dto.getAppointmentId())
                .orElseThrow(() -> new NotFoundException("Appointment not found"));

        assertMemberOwnsAppointment(appointment, userId);
        assertCancelable(appointment);

        applyCancellation(appointment, dto.getCancelReason());

        Appointment saved = appointmentRepository.save(appointment);

        sendCancellationEmailsAfterCommit(saved, false);

        return appointmentDtoMapper.toDTO(saved);
    }

    @Transactional
    public AppointmentResponseDTO cancelGuestAppointment(String token, String reason) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Missing cancellation token");
        }

        Appointment appointment = appointmentRepository.findByGuestCancelTokenForUpdate(token)
                .orElseThrow(() -> new NotFoundException("No appointment found for this cancel token"));

        assertGuestAppointment(appointment);
        assertGuestCancelTokenValid(appointment);
        assertCancelable(appointment);

        applyCancellation(appointment, reason);

        appointment.setGuestCancelToken(null);
        appointment.setGuestTokenExpiresAt(null);

        Appointment saved = appointmentRepository.save(appointment);

        sendCancellationEmailsAfterCommit(saved, true);

        return appointmentDtoMapper.toDTO(saved);
    }

    public AppointmentResponseDTO getAppointmentById(UUID appointmentId){
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found"));
        log.info("Retrieved service with ID: {}", appointment.getId());
        return appointmentDtoMapper.toDTO(appointment);
    }

    public AppointmentResponseDTO getGuestAppointmentByToken(String token) {
        Appointment appointment = appointmentRepository.findByGuestCancelToken(token)
                .orElseThrow(() -> new NotFoundException("Appointment not found"));
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
                AppointmentStatus.CONFIRMED
        );

        return appointmentRepository.findFirstByUserIdAndAppointmentTimeAfterAndAppointmentStatusInOrderByAppointmentTimeAsc(userId, now, statuses)
                .map(appointmentDtoMapper::toSummaryDTO);
    }

    public Page<AppointmentSummaryDTO> getPreviousAppointments(UUID userId, Pageable pageable){

        List<AppointmentStatus> statuses = List.of(
                AppointmentStatus.COMPLETED,
                AppointmentStatus.CANCELED,
                AppointmentStatus.NO_SHOW
        );

        return appointmentRepository
                .findByUser_IdAndAppointmentStatusInOrderByAppointmentTimeDesc(userId, statuses, pageable)
                .map(appointmentDtoMapper::toSummaryDTO);
    }

    private void assertNoConflicts(Appointment appointment, int bufferMinutes) {
        LocalDateTime start = appointment.getAppointmentTime();
        LocalDateTime end = start.plusMinutes(appointment.getDurationMinutes() + bufferMinutes);
        List<Appointment> conflicts = appointmentRepository.findConflictingAppointments(start, end, bufferMinutes);
        if (!conflicts.isEmpty()) {
            throw new ConflictException("This time overlaps with another appointment.");
        }
    }

    private void attachServiceAndAddOns(Appointment appointment, ServiceModel service, AppointmentRequestDTO dto) {
        appointment.setService(service);

        List<AddOn> addOns = new ArrayList<>();
        if (dto.getAddOnIds() != null && !dto.getAddOnIds().isEmpty()) {
            addOns = addOnService.getAddOnIds(dto.getAddOnIds());
        }
        appointment.setAddOns(addOns);

        int addOnMinutes = addOns.stream()
                .map(AddOn::getDurationMinutes)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        int durationMinutes = (service.getDurationMinutes() == null ? 0 : service.getDurationMinutes()) + addOnMinutes;
        appointment.setDurationMinutes(durationMinutes);
    }

    private void attachUserOrGuest(Appointment appointment, AppointmentRequestDTO dto, AppUserPrincipal principal) {
        if (principal != null) {
            User user = userRepository.findById(principal.id())
                    .orElseThrow(() -> new NotFoundException("No user found with ID: " + principal.id()));
            appointment.setUser(user);
            appointment.setGuestEmail(null);
        } else {
            String email = dto.getReceiptEmail();
            if (email == null || email.isBlank()) {
                throw new BadRequestException("Guest booking requires a receipt email");
            }
            appointment.setUser(null);
            appointment.setGuestEmail(email);
            appointment.setGuestCancelToken(UUID.randomUUID().toString());
            appointment.setGuestTokenExpiresAt(appointment.getAppointmentTime()); // keeping your current behavior
        }
    }

    private boolean isAppointmentTimeUniqueViolation(DataIntegrityViolationException ex) {
        Throwable t = ex;
        while (t != null) {
            if (t instanceof ConstraintViolationException cve) {
                return "uk_appointment_time".equals(cve.getConstraintName());
            }
            t = t.getCause();
        }
        return false;
    }

    private void runAfterCommit(Runnable work) {
        if (work == null) return;

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    work.run();
                }
            });
        } else {
            work.run();
        }
    }

    private void assertCancelable(Appointment appointment) {
        if (appointment.getAppointmentStatus() == AppointmentStatus.CANCELED) {
            throw new ConflictException("Appointment already canceled");
        }
        if (appointment.getAppointmentStatus() == AppointmentStatus.COMPLETED) {
            throw new ConflictException("Can not cancel an appointment that has already been completed");
        }
        if (appointment.getAppointmentStatus() == AppointmentStatus.NO_SHOW) {
            throw new ConflictException("Appointment has already been marked as No Show meaning this appointment is already effectively cancelled");
        }
    }

    private void assertGuestCancelTokenValid(Appointment appointment) {
        LocalDateTime now = LocalDateTime.now();

        if (appointment.getGuestCancelToken() == null || appointment.getGuestCancelToken().isBlank()) {
            throw new ConflictException("This cancellation link has expired.");
        }

        LocalDateTime expiresAt = appointment.getGuestTokenExpiresAt();
        if (expiresAt == null || now.isAfter(expiresAt)) {
            throw new ConflictException("This cancellation link has expired.");
        }
    }

    private void assertGuestAppointment(Appointment appointment) {
        if (appointment.getUser() != null) {
            throw new UnauthorizedException("This cancel token is not valid for member appointments");
        }
    }

    private void assertMemberOwnsAppointment(Appointment appointment, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (appointment.getUser() == null) {
            throw new UnauthorizedException("Guest appointments cannot be canceled via this endpoint");
        }
        if (!appointment.getUser().getId().equals(userId) && !user.getUserType().equals(UserType.ADMIN)) {
            throw new UnauthorizedException("You cannot cancel someone else's appointment");
        }
    }

    private void applyCancellation(Appointment appointment, String reason) {
        if (reason != null && !reason.isBlank()) {
            appointment.setCancelReason(reason.trim());
        }

        appointment.setAppointmentStatus(AppointmentStatus.CANCELED);
        appointment.setHoldExpiresAt(null);
        appointment.setUpdatedAt(LocalDateTime.now());
    }

    private List<String> addOnNames(Appointment appt) {
        if (appt.getAddOns() == null) return List.of();
        return appt.getAddOns().stream()
                .map(AddOn::getName)
                .filter(Objects::nonNull)
                .toList();
    }

    private String displayCustomerName(Appointment appointment) {
        if (appointment.getUser() != null && appointment.getUser().getName() != null && !appointment.getUser().getName().isBlank()) {
            return appointment.getUser().getName();
        }
        return "Guest";
    }

    private String customerEmail(Appointment appointment) {
        if (appointment.getUser() != null) return appointment.getUser().getEmail();
        return appointment.getGuestEmail();
    }

    private Map<String, Object> buildCustomerCancelModel(Appointment appointment, boolean isGuest) {
        Map<String, Object> m = new HashMap<>();

        m.put("customerName", displayCustomerName(appointment));
        m.put("appointmentDateTime", appointment.getAppointmentTime());
        m.put("serviceName", appointment.getService().getName());
        m.put("addOns", addOnNames(appointment));
        m.put("customerNote", appointment.getNote());
        m.put("cancelReason", appointment.getCancelReason());
        m.put("depositAmount", appointment.getDepositAmount());
        m.put("isGuest", isGuest);
        m.put("bookUrl", frontendProps.baseUrl() + "/categories");
        m.put("memberManageUrl", frontendProps.baseUrl());

        return m;
    }

    private Map<String, Object> buildAdminCancelModel(Appointment appointment) {
        Map<String, Object> m = new HashMap<>();

        m.put("appointmentDateTime", appointment.getAppointmentTime());
        m.put("serviceName", appointment.getService().getName());
        m.put("addOns", addOnNames(appointment));
        m.put("customerName", displayCustomerName(appointment));
        m.put("customerEmail", customerEmail(appointment));
        m.put("depositAmount", appointment.getDepositAmount());
        m.put("customerNote", appointment.getNote());
        m.put("cancelReason", appointment.getCancelReason());
        m.put("adminDashboardUrl", frontendProps.baseUrl());

        return m;
    }

    private Map<String, Object> buildCustomerDepositModel(Appointment appointment) {
        boolean isGuest = appointment.getUser() == null;

        String guestCancelUrl = frontendProps.baseUrl() + "/guest/cancel?token=" + appointment.getGuestCancelToken();
        String memberManageUrl = frontendProps.baseUrl() + "/dashboard/me/appointments";

        Map<String, Object> m = new HashMap<>();
        m.put("customerName", displayCustomerName(appointment));
        m.put("appointmentDateTime", appointment.getAppointmentTime());
        m.put("serviceName", appointment.getService().getName());
        m.put("depositAmount", appointment.getDepositAmount());
        m.put("remainingAmount", appointment.getRemainingBalance());
        m.put("isGuest", isGuest);
        m.put("guestCancelUrl", guestCancelUrl);
        m.put("memberManageUrl", memberManageUrl);
        m.put("noDepositRequired", true);
        return m;
    }

    private Map<String, Object> buildAdminNewAppointmentModel(Appointment appointment) {
        boolean isGuest = appointment.getUser() == null;

        String adminAppointmentUrl = frontendProps.baseUrl() + "dashboard/admin/appointments/" + appointment.getId();

        Map<String, Object> m = new HashMap<>();
        m.put("appointmentDateTime", appointment.getAppointmentTime());
        m.put("serviceName", appointment.getService().getName());
        m.put("addOns", addOnNames(appointment));
        m.put("clientType", isGuest ? "Guest" : "Member");
        m.put("customerName", displayCustomerName(appointment));
        m.put("customerEmail", customerEmail(appointment));
        m.put("depositAmount", appointment.getDepositAmount());
        m.put("customerNote", appointment.getNote());
        m.put("adminAppointmentUrl", adminAppointmentUrl);
        return m;
    }

    private void sendCancellationEmailsAfterCommit(Appointment appointment, boolean isGuest) {
        // capture everything now (stable), then send after commit
        String customerEmail = customerEmail(appointment);
        String adminEmail = businessSettingsService.getOrCreate().getCompanyEmail();

        Map<String, Object> customerModel = buildCustomerCancelModel(appointment, isGuest);
        Map<String, Object> adminModel = buildAdminCancelModel(appointment);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d yyyy h:mm a");
        String formattedDate = appointment.getAppointmentTime().format(formatter);

        Runnable emailWork = () -> {
            try {
                String customerHtml = emailTemplateService.render("cancel-confirmation", customerModel);
                emailService.sendHtmlEmail(customerEmail, "Appointment canceled", customerHtml);

                String adminHtml = emailTemplateService.render("admin-cancel-notification", adminModel);
                emailService.sendHtmlEmail(adminEmail, "Appointment canceled - " + formattedDate, adminHtml);
            } catch (Exception ex) {
                log.error("Post-commit cancellation email failed. apptId={} msg={}", appointment.getId(), ex.getMessage(), ex);
            }
        };

        runAfterCommit(emailWork);
    }

    private void sendNoDepositBookingEmailsAfterCommit(Appointment appointment) {
        String customerEmail = customerEmail(appointment);
        String adminEmail = businessSettingsService.getOrCreate().getCompanyEmail();

        Map<String, Object> customerModel = buildCustomerDepositModel(appointment);
        Map<String, Object> adminModel = buildAdminNewAppointmentModel(appointment);

        Runnable work = () -> {
            try {
                String customerHtml = emailTemplateService.render("deposit-receipt", customerModel);
                String adminHtml = emailTemplateService.render("admin-new-apt-notification", adminModel);

                emailService.sendHtmlEmail(customerEmail, "Appointment confirmed", customerHtml);
                emailService.sendHtmlEmail(
                        adminEmail,
                        "New Appointment Booked - " + appointment.getAppointmentTime(),
                        adminHtml
                );
            } catch (Exception ex) {
                log.error("Post-commit no-deposit booking email failed. apptId={} msg={}",
                        appointment.getId(), ex.getMessage(), ex);
            }
        };

        runAfterCommit(work);
    }
}
