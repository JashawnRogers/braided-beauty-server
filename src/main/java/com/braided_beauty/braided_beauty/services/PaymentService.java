package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.enums.*;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.models.*;
import com.braided_beauty.braided_beauty.records.EmailAddOnLine;
import com.braided_beauty.braided_beauty.records.FrontendProps;
import com.braided_beauty.braided_beauty.repositories.AppointmentRepository;
import com.braided_beauty.braided_beauty.repositories.PaymentRepository;
import com.braided_beauty.braided_beauty.repositories.ServiceRepository;
import com.braided_beauty.braided_beauty.utils.PhoneNormalizer;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.String;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AppointmentRepository appointmentRepository;
    private final ServiceRepository serviceRepository;
    private final LoyaltyService loyaltyService;
    private final AppointmentConfirmationService appointmentConfirmationService;
    private final EmailTemplateService emailTemplateService;
    private final EmailService emailService;
    private final BusinessSettingsService businessSettingsService;
    private final FrontendProps frontendProps;

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Transactional
    public void updateRefundPayment(Appointment appointment) throws StripeException {
        Payment payment = paymentRepository.findByAppointment_IdAndPaymentType(appointment.getId(), PaymentType.FINAL)
                .orElseThrow(() -> new NotFoundException("No payment found for appointment."));

        if (payment.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw new IllegalStateException("Refund already processed for appointment " + appointment.getId());
        }

        RefundCreateParams refundParams = RefundCreateParams.builder()
                .setPaymentIntent(payment.getStripePaymentIntentId())
                .build();

        Refund.create(refundParams);

        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        log.info("Refund successful for appointment ID: {}", appointment.getId());
    }

    // Session is created to be sent to webhook
    @Transactional
    public Session createDepositCheckoutSession(Appointment appointment, String successUrl, String cancelUrl) throws StripeException {
        Objects.requireNonNull(appointment, "appointment is required");
        Objects.requireNonNull(appointment.getId(), "appointment ID is required (persist before calling Stripe)");

        // Idempotency guard: if a deposit payment already exists for this appointment, reuse the existing session
        Optional<Payment> existingDepositOpt = paymentRepository.findByAppointment_IdAndPaymentType(appointment.getId(), PaymentType.DEPOSIT);
        if (existingDepositOpt.isPresent()) {
            Payment existing = existingDepositOpt.get();

            if (existing.getPaymentStatus() == PaymentStatus.PAID_DEPOSIT
                    || existing.getPaymentStatus() == PaymentStatus.REFUNDED
                    || existing.getPaymentStatus() == PaymentStatus.PAID_IN_FULL) {
                throw new IllegalStateException("Deposit already processed for appointment " + appointment.getId());
            }

            if (existing.getStripeSessionId() != null && existing.getPaymentStatus() == PaymentStatus.PENDING_PAYMENT) {
                log.info("Reusing existing deposit checkout session {} for appointment {}", existing.getStripeSessionId(), appointment.getId());
                return Session.retrieve(existing.getStripeSessionId());
            }
        }

        BigDecimal deposit = Objects.requireNonNullElse(appointment.getDepositAmount(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        if (deposit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("No deposit required for appointment " + appointment.getId());
        }

        String email = appointment.getUser() != null ? appointment.getUser().getEmail() : appointment.getGuestEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Missing customer email for appointment " + appointment.getId());
        }

        long depositCents = deposit.movePointRight(2).longValueExact();

        String addOnNames = appointment.getAddOns() == null ? "" :
                appointment.getAddOns().stream()
                        .map(AddOn::getName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(","));

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .setCustomerEmail(email)
                .putMetadata("appointmentId", appointment.getId().toString())
                .putMetadata("paymentType", "deposit")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("usd")
                                                .setUnitAmount(depositCents)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Deposit - " + appointment.getService().getName())
                                                                .build())
                                                .build())
                                .build()
                )
                .setPaymentIntentData(
                        SessionCreateParams.PaymentIntentData.builder()
                                .putMetadata("appointmentId", appointment.getId().toString())
                                .putMetadata("userId", appointment.getUser() != null ? appointment.getUser().getId().toString() : "guest")
                                .putMetadata("addOnNames", addOnNames)
                                .putMetadata("paymentType", "deposit")
                                .build()
                )
                .build();

        Session session = Session.create(params);

        Payment payment = Payment.builder()
                .stripeSessionId(session.getId())
                .stripePaymentIntentId(session.getPaymentIntent())
                .amount(deposit)
                .paymentStatus(PaymentStatus.PENDING_PAYMENT)
                .paymentMethod(PaymentMethod.CARD)
                .paymentType(PaymentType.DEPOSIT)
                .appointment(appointment)
                .user(appointment.getUser())
                .build();

        paymentRepository.save(payment);
        log.info("Created Stripe checkout session for deposit. Session ID: {}", session.getId());

        return session;
    }

    @Transactional
    public Session createFinalPaymentSession(
            Appointment appointment,
            String successUrl,
            String cancelUrl,
            BigDecimal tipAmount
    ) throws StripeException {
        // This method should be responsible for creating the Stripe session + payment row.

        tipAmount = Objects.requireNonNullElse(tipAmount, BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        // --- idempotency guard ---
        Optional<Payment> existingFinalOpt =
                paymentRepository.findByAppointment_IdAndPaymentType(appointment.getId(), PaymentType.FINAL);

        if (existingFinalOpt.isPresent()) {
            Payment existing = existingFinalOpt.get();
            if (existing.getPaymentStatus() == PaymentStatus.PAID_IN_FULL
                    || existing.getPaymentStatus() == PaymentStatus.REFUNDED) {
                throw new IllegalStateException("Final payment already processed for appointment " + appointment.getId());
            }
            if (existing.getPaymentStatus() == PaymentStatus.PENDING_PAYMENT && existing.getStripeSessionId() != null) {
                log.info("Reusing existing final-payment checkout session {} for appointment {}",
                        existing.getStripeSessionId(), appointment.getId());
                return Session.retrieve(existing.getStripeSessionId());
            }
        }

        BigDecimal remaining = Objects.requireNonNullElse(appointment.getRemainingBalance(), BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("No remaining balance to pay");
        }

        String email = appointment.getUser() != null ? appointment.getUser().getEmail() : appointment.getGuestEmail();

        long remainingCents = remaining.movePointRight(2).longValueExact();

        List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();

        // Remaining balance line item
        lineItems.add(
                SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(
                                SessionCreateParams.LineItem.PriceData.builder()
                                        .setCurrency("usd")
                                        .setUnitAmount(remainingCents)
                                        .setProductData(
                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                        .setName("Remaining balance: " + appointment.getService().getName())
                                                        .build()
                                        )
                                        .build()
                        )
                        .build()
        );

        // Tip line item (optional)
        if (tipAmount.compareTo(BigDecimal.ZERO) > 0) {
            long tipCents = tipAmount.movePointRight(2).longValueExact();
            lineItems.add(
                    SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(
                                    SessionCreateParams.LineItem.PriceData.builder()
                                            .setCurrency("usd")
                                            .setUnitAmount(tipCents)
                                            .setProductData(
                                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                            .setName("Tip")
                                                            .build()
                                            )
                                            .build()
                            )
                            .build()
            );
        }

        BigDecimal finalAmountCharged = remaining.add(tipAmount).setScale(2, RoundingMode.HALF_UP);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .setCustomerEmail(email)
                .putMetadata("appointmentId", appointment.getId().toString())
                .putMetadata("paymentType", "final")
                .addAllLineItem(lineItems)
                .setPaymentIntentData(
                        SessionCreateParams.PaymentIntentData.builder()
                                .putMetadata("appointmentId", appointment.getId().toString())
                                .putMetadata("userId", appointment.getUser() != null ? appointment.getUser().getId().toString() : "guest")
                                .putMetadata("paymentType", "final")
                                .putMetadata("tipAmount", tipAmount.toString())
                                .putMetadata("remainingBalanceDue", remaining.toString())
                                .putMetadata("finalAmountCharged", finalAmountCharged.toString())
                                .build()
                )
                .build();

        Session session = Session.create(params);

        Payment payment = Payment.builder()
                .stripeSessionId(session.getId())
                .stripePaymentIntentId(session.getPaymentIntent())
                .amount(finalAmountCharged) // Stripe charge = remaining + tip
                .tipAmount(tipAmount)
                .paymentStatus(PaymentStatus.PENDING_PAYMENT)
                .paymentMethod(PaymentMethod.CARD)
                .paymentType(PaymentType.FINAL)
                .appointment(appointment)
                .user(appointment.getUser())
                .build();

        paymentRepository.save(payment);

        log.info("Created Stripe checkout session for final payment. Session ID: {}", session.getId());
        return session;
    }

    @Transactional
    public void handleCheckoutSessionCompleted(Session session) {
        String paymentType = session.getMetadata().get("paymentType");
        String appointmentId = session.getMetadata().get("appointmentId");

        if (paymentType == null || appointmentId == null) {
            log.warn("Missing metadata on Checkout Session {}: appointmentId/paymentType", session.getId());
            throw new IllegalStateException("payment type cannot be null: " + paymentType + ". appointmentId: " + appointmentId);
        }

        Appointment appointment = appointmentRepository.findById(UUID.fromString(appointmentId))
                .orElseThrow(() -> new NotFoundException("Appointment not found: " + appointmentId));

        List<AddOn> addOns = appointment.getAddOns();
        ServiceModel service = appointment.getService();

        BigDecimal addOnsTotal = addOns.stream()
                .map(AddOn::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal total = addOnsTotal
                .add(service.getPrice())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal deposit = total
                .multiply(new BigDecimal("0.20"))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal remainingBalance = total.subtract(deposit).setScale(2, RoundingMode.HALF_UP);

        String email = appointment.getUser() != null ? appointment.getUser().getEmail() : appointment.getGuestEmail();

        BusinessSettings settings = businessSettingsService.getOrCreate();
        String companyPhoneNumber  = PhoneNormalizer.formatForEmail(settings.getCompanyPhoneNumber());

        log.info("Payment Type: {}", paymentType);

        // Idempotency: if the appointment is already in the terminal state for the given payment type, exit.
        if ("deposit".equals(paymentType) && appointment.getPaymentStatus() == PaymentStatus.PAID_DEPOSIT) {
            log.info("Deposit already processed for appointment {}", appointmentId);
            return;
        }
        if ("final".equals(paymentType)
                && (appointment.getAppointmentStatus() == AppointmentStatus.COMPLETED
                || appointment.getPaymentStatus() == PaymentStatus.PAID_IN_FULL)) {
            log.info("Final payment already processed for appointment {}", appointmentId);
            return;
        }

        Payment payment = paymentRepository.findByStripeSessionId(session.getId())
                .orElseThrow(() -> new NotFoundException("Payment not found for session: " + session.getId()));

        // Idempotency: if this payment row already reflects a successful processing, do nothing.
        if (payment.getPaymentStatus() == PaymentStatus.PAID_DEPOSIT
                || payment.getPaymentStatus() == PaymentStatus.PAID_IN_FULL
                || payment.getPaymentStatus() == PaymentStatus.REFUNDED) {
            log.info("Payment already processed for session {} (status={})", session.getId(), payment.getPaymentStatus());
            return;
        }

        List<EmailAddOnLine> addOnLines = appointment.getAddOns().stream()
                .map(a -> new EmailAddOnLine(a.getName(), a.getPrice().toString()))
                .toList();

        String customerName = (appointment.getUser() != null ? appointment.getUser().getName() : "Guest");

        Map<String, Object> base = new HashMap<>();
        base.put("customerName", customerName);
        base.put("appointmentDateTime", appointment.getAppointmentTime());
        base.put("serviceName", appointment.getService().getName());
        base.put("depositAmount", appointment.getDepositAmount());
        base.put("businessPhone", companyPhoneNumber);
        base.put("businessAddress",settings.getCompanyAddress());

        // Persist state changes first. Email failures must not rollback payment/appointment updates.
        Runnable afterCommitEmailWork = null;

        switch (payment.getPaymentType()) {
            case DEPOSIT -> {
                payment.setPaymentStatus(PaymentStatus.PAID_DEPOSIT);
                payment.setPaymentMethod(PaymentMethod.CARD);
                payment.setStripePaymentIntentId(session.getPaymentIntent());
                payment.setStripeSessionId(session.getId());
                paymentRepository.save(payment);

                String clientType = appointment.getUser() != null ? "Member" : "Guest";

                appointment.setStripeSessionId(session.getId());
                appointment.setAppointmentStatus(AppointmentStatus.CONFIRMED);
                appointment.setPaymentStatus(PaymentStatus.PAID_DEPOSIT);
                appointment.setHoldExpiresAt(null);
                appointment.setRemainingBalance(remainingBalance);
                appointment.setLoyaltyApplied(false);
                Appointment saved = appointmentRepository.save(appointment);
                service.setTimesBooked(service.getTimesBooked() + 1);
                serviceRepository.save(service);

                Map<String, Object> depositModel = new HashMap<>(base);
                depositModel.put("remainingAmount", remainingBalance);
                depositModel.put("isGuest", clientType.equals("Guest"));
                depositModel.put("noDepositRequired", false);
                if (clientType.equals("Guest")) {
                    depositModel.put("guestCancelUrl", frontendProps.baseUrl() + "/guest/cancel/" + appointment.getGuestCancelToken());
                } else {
                    depositModel.put("memberManageUrl", frontendProps.baseUrl());
                }

                Map<String, Object> adminModel = new HashMap<>();
                adminModel.put("appointmentDateTime", saved.getAppointmentTime());
                adminModel.put("clientType", clientType);
                adminModel.put("serviceName", saved.getService().getName());
                adminModel.put("addOns", saved.getAddOns().stream()
                        .map(AddOn::getName)
                        .filter(Objects::nonNull)
                        .toList());
                adminModel.put("customerName", customerName);
                adminModel.put("customerEmail", email);
                adminModel.put("depositAmount", saved.getDepositAmount() != null ? saved.getDepositAmount() : BigDecimal.ZERO);
                adminModel.put("customerNote", saved.getNote() != null ? saved.getNote() : "");
                adminModel.put("adminAppointmentUrl", frontendProps.baseUrl() + "dashboard/admin/appointments/" + appointment.getId());

                String adminEmail = businessSettingsService.getOrCreate().getCompanyEmail();

                // Build email work to run AFTER COMMIT.
                afterCommitEmailWork = () -> {
                    try {
                        String html = emailTemplateService.render("deposit-receipt", depositModel);
                        String adminHtml = emailTemplateService.render("admin-new-apt-notification", adminModel);
                        emailService.sendHtmlEmail(email, "Deposit confirmation", html);
                        emailService.sendHtmlEmail(
                                adminEmail,
                                "New Appointment Booked - " + appointment.getAppointmentTime().toLocalDate() + " " + appointment.getAppointmentTime().toLocalTime(),
                                    adminHtml
                        );
                        log.info("Emails sent!");

                        // Token creation is helpful but must not prevent confirmation.
                        appointmentConfirmationService.ensureConfirmationTokenForAppointment(saved.getId());
                    } catch (Exception ex) {
                        log.error("Post-commit deposit email/token work failed. sessionId={} apptId={} msg={}",
                                session.getId(), appointmentId, ex.getMessage(), ex);
                    }
                };

                log.info("Deposit completed via checkout session {} for appointment {}", session.getId(), appointmentId);
            }
            case FINAL -> {
                // capture BEFORE you mutate appointment
                BigDecimal remainingBeforeDiscount =
                        Objects.requireNonNullElse(appointment.getRemainingBalance(), BigDecimal.ZERO);

                BigDecimal serviceAmount = Objects.requireNonNullElse(appointment.getService().getPrice(), BigDecimal.ZERO);
                BigDecimal subtotal = addOnsTotal.add(serviceAmount);

                BigDecimal discountAmount = Objects.requireNonNullElse(appointment.getDiscountAmount(), BigDecimal.ZERO);
                BigDecimal depositAmount = Objects.requireNonNullElse(appointment.getDepositAmount(), BigDecimal.ZERO);
                BigDecimal tip = Objects.requireNonNullElse(appointment.getTipAmount(), BigDecimal.ZERO);
                int discountPercent = Objects.requireNonNullElse(appointment.getDiscountPercent(), 0);

                BigDecimal chargedToday = remainingBeforeDiscount.subtract(discountAmount).add(tip);
                if (chargedToday.compareTo(BigDecimal.ZERO) < 0) chargedToday = BigDecimal.ZERO;

                BigDecimal totalPaid = depositAmount.add(chargedToday);

                // NOW update Payment row
                payment.setPaymentStatus(PaymentStatus.PAID_IN_FULL);
                payment.setStripePaymentIntentId(session.getPaymentIntent());
                payment.setStripeSessionId(session.getId());
                payment.setPaymentMethod(PaymentMethod.CARD);
                paymentRepository.save(payment);

                // NOW update Appointment
                appointment.setAppointmentStatus(AppointmentStatus.COMPLETED);
                appointment.setPaymentStatus(PaymentStatus.PAID_IN_FULL);
                appointment.setRemainingBalance(BigDecimal.ZERO);
                appointment.setCompletedAt(LocalDateTime.now());

                if (appointment.getUser() == null) {
                    log.info("Skipping loyalty reward (guest appointment {})", appointmentId);
                } else if (!appointment.isLoyaltyApplied()) {
                    if (appointment.getUser().getLoyaltyRecord() == null) {
                        loyaltyService.attachLoyaltyRecord(appointment.getUser().getId());
                    }
                    loyaltyService.awardForCompletedAppointment(appointment.getUser().getId());
                    appointment.setLoyaltyApplied(true);
                }

                appointmentRepository.save(appointment);

                Map<String, Object> finalModel = new HashMap<>(base);
                finalModel.put("serviceAmount", serviceAmount);
                finalModel.put("addOns", addOnLines);
                finalModel.put("subtotal", subtotal);
                finalModel.put("discountAmount", discountAmount);
                finalModel.put("discountPercent", discountPercent);
                finalModel.put("depositAmount", depositAmount);
                finalModel.put("tipAmount", tip);
                finalModel.put("chargedToday", chargedToday);
                finalModel.put("totalPaid", totalPaid);


                afterCommitEmailWork = () -> {
                    try {
                        String html = emailTemplateService.render("final-payment-receipt", finalModel);
                        emailService.sendHtmlEmail(email, "Final Payment Received", html);
                    } catch (Exception ex) {
                        log.error("Post-commit final email work failed. sessionId={} apptId={} msg={}",
                                session.getId(), appointmentId, ex.getMessage(), ex);
                    }
                };

                log.info("Final payment completed via session: {} for appointment: {}", session.getId(), appointmentId);
            }
        }

        // Run email work after successful DB commit to avoid breaking idempotency.
        if (afterCommitEmailWork != null && TransactionSynchronizationManager.isSynchronizationActive()) {
            Runnable finalAfterCommitEmailWork = afterCommitEmailWork;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    finalAfterCommitEmailWork.run();
                }
            });
        } else if (afterCommitEmailWork != null) {
            // Fallback (should be rare): run immediately but still swallow errors inside the runnable.
            afterCommitEmailWork.run();
        }
    }

    @Transactional
    public void handleCheckoutSessionFailed(Session session) {
        String appointmentId = session.getMetadata().get("appointmentId");
        String paymentType = session.getMetadata().get("paymentType");

        if (appointmentId == null || paymentType == null) {
            log.warn("Missing metadata on failed session session: {}", session.getId());
            return;
        }

        Appointment appointment = appointmentRepository.findById(UUID.fromString(appointmentId))
                        .orElseThrow(() -> new NotFoundException("Appointment not found."));

        Payment payment = paymentRepository.findByStripeSessionId(session.getId())
                        .orElseThrow(() -> new NotFoundException("Payment not found for session: " + session.getId()));

        if (payment.getPaymentStatus() == PaymentStatus.PAYMENT_FAILED) return;

        payment.setPaymentStatus(PaymentStatus.PAYMENT_FAILED);
        paymentRepository.save(payment);

        appointment.setPaymentStatus(PaymentStatus.PAYMENT_FAILED);
        appointment.setAppointmentStatus(AppointmentStatus.CANCELED);
        appointment.setCancelReason("Payment failed / abandoned checkout");
        appointment.setHoldExpiresAt(null);
        appointmentRepository.save(appointment);
        log.info("Appointment {} marked as PAYMENT_FAILED. / paymentIntent {}", appointmentId, session.getPaymentIntent());
    }

}
