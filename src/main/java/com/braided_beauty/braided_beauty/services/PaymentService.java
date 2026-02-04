package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import com.braided_beauty.braided_beauty.enums.PaymentStatus;
import com.braided_beauty.braided_beauty.enums.PaymentType;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.models.*;
import com.braided_beauty.braided_beauty.records.EmailAddOnLine;
import com.braided_beauty.braided_beauty.records.FrontendProps;
import com.braided_beauty.braided_beauty.repositories.AppointmentRepository;
import com.braided_beauty.braided_beauty.repositories.PaymentRepository;
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
import java.util.*;
import java.util.stream.Stream;

@AllArgsConstructor
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AppointmentRepository appointmentRepository;
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

    // Session are created to be sent to webhook
    @Transactional
    public Session createDepositCheckoutSession(Appointment appointment, String successUrl, String cancelUrl) throws StripeException {
        // Idempotency guard: if a deposit payment already exists for this appointment, reuse the existing session.
        paymentRepository.findByAppointment_IdAndPaymentType(appointment.getId(), PaymentType.DEPOSIT)
                .ifPresent(existing -> {
                    if (existing.getPaymentStatus() == PaymentStatus.PAID_DEPOSIT
                            || existing.getPaymentStatus() == PaymentStatus.REFUNDED
                            || existing.getPaymentStatus() == PaymentStatus.PAID_IN_FULL_ACH) {
                        throw new IllegalStateException("Deposit already processed for appointment " + appointment.getId());
                    }
                });

        Optional<Payment> existingDepositOpt = paymentRepository.findByAppointment_IdAndPaymentType(appointment.getId(), PaymentType.DEPOSIT);
        if (existingDepositOpt.isPresent() && existingDepositOpt.get().getStripeSessionId() != null
                && existingDepositOpt.get().getPaymentStatus() == PaymentStatus.PENDING_PAYMENT) {
            log.info("Reusing existing deposit checkout session {} for appointment {}", existingDepositOpt.get().getStripeSessionId(), appointment.getId());
            return Session.retrieve(existingDepositOpt.get().getStripeSessionId());
        }

        BigDecimal deposit = appointment.getDepositAmount();
        String email = appointment.getUser() != null ? appointment.getUser().getEmail() : appointment.getGuestEmail();

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
                                                .setUnitAmount(deposit.movePointRight(2).longValue())
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(appointment.getService().getName())
                                                                .build())
                                                .build())
                                .build()
                )
                .setPaymentIntentData(
                        SessionCreateParams.PaymentIntentData.builder()
                                .putMetadata("appointmentId", appointment.getId().toString())
                                .putMetadata("userId", appointment.getUser() != null ? appointment.getUser().getId().toString() : "guest")
                                .putMetadata("addOnIds", appointment.getAddOns().toString())
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
                .paymentType(PaymentType.DEPOSIT)
                .appointment(appointment)
                .user(appointment.getUser())
                .build();

        paymentRepository.save(payment);
        log.info("Created Stripe checkout session for deposit. Session ID: {}", session.getId());

        return session;
    }

    @Transactional
    public Session createFinalPaymentSession(Appointment appointment, String successUrl, String cancelUrl, BigDecimal tipAmount) throws StripeException {
        if (tipAmount == null) {
            tipAmount = BigDecimal.ZERO;
        }

        // Idempotency guard: reuse an existing pending final-payment session; block if already paid.
        Optional<Payment> existingFinalOpt = paymentRepository.findByAppointment_IdAndPaymentType(appointment.getId(), PaymentType.FINAL);
        if (existingFinalOpt.isPresent()) {
            Payment existing = existingFinalOpt.get();
            if (existing.getPaymentStatus() == PaymentStatus.PAID_IN_FULL_ACH
                    || existing.getPaymentStatus() == PaymentStatus.REFUNDED) {
                throw new IllegalStateException("Final payment already processed for appointment " + appointment.getId());
            }
            if (existing.getPaymentStatus() == PaymentStatus.PENDING_PAYMENT && existing.getStripeSessionId() != null) {
                log.info("Reusing existing final-payment checkout session {} for appointment {}", existing.getStripeSessionId(), appointment.getId());
                return Session.retrieve(existing.getStripeSessionId());
            }
        }

        BigDecimal remainingBalance = Objects.requireNonNullElse(appointment.getRemainingBalance(), BigDecimal.ZERO);
        BigDecimal finalAmount = remainingBalance.add(tipAmount);
        appointment.setTotalAmount(appointment.getTotalAmount().add(tipAmount));
        String email = appointment.getUser() != null ? appointment.getUser().getEmail() : appointment.getGuestEmail();

        if (remainingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("No remaining balance to pay");
        }

        List<SessionCreateParams.LineItem> lineItems = Stream.of(
                SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(
                                SessionCreateParams.LineItem.PriceData.builder()
                                        .setCurrency("usd")
                                        .setUnitAmount(remainingBalance.movePointRight(2).longValueExact())
                                        .setProductData(
                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                        .setName("Remaining balance: " + appointment.getService().getName())
                                                        .build()
                                        )
                                        .build()
                        )
                        .build(),

                // Tip amount line item (optional)
                tipAmount.compareTo(BigDecimal.ZERO) > 0 ?
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("usd")
                                                .setUnitAmount(tipAmount.movePointRight(2).longValueExact())
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Tip")
                                                                .build()
                                                )
                                                .build()
                                )
                                .build() : null
        ).filter(Objects::nonNull).toList(); // Prevents null line item crash

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
                            .putMetadata("tipAmount", tipAmount.toString())
                            .putMetadata("paymentType", "final")
                            .putMetadata("remainingBalance", remainingBalance.toString())
                            .putMetadata("finalAmount", finalAmount.toString())
                            .build()
                )
                .build();

        if (lineItems.isEmpty()) {
            throw new IllegalArgumentException("No amount due for appointment " + appointment.getId());
        }

        Session session = Session.create(params);

        Payment payment = Payment.builder()
                .stripeSessionId(session.getId())
                .stripePaymentIntentId(session.getPaymentIntent())
                .amount(finalAmount)
                .tipAmount(tipAmount)
                .paymentStatus(PaymentStatus.PENDING_PAYMENT)
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
                || appointment.getPaymentStatus() == PaymentStatus.PAID_IN_FULL_ACH)) {
            log.info("Final payment already processed for appointment {}", appointmentId);
            return;
        }

        Payment payment = paymentRepository.findByStripeSessionId(session.getId())
                .orElseThrow(() -> new NotFoundException("Payment not found for session: " + session.getId()));

        // Idempotency: if this payment row already reflects a successful processing, do nothing.
        if (payment.getPaymentStatus() == PaymentStatus.PAID_DEPOSIT
                || payment.getPaymentStatus() == PaymentStatus.PAID_IN_FULL_ACH
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
                paymentRepository.save(payment);

                appointment.setStripeSessionId(session.getId());
                appointment.setAppointmentStatus(AppointmentStatus.CONFIRMED);
                appointment.setPaymentStatus(PaymentStatus.PAID_DEPOSIT);
                appointment.setHoldExpiresAt(null);
                appointment.setRemainingBalance(remainingBalance);
                appointment.setLoyaltyApplied(false);
                Appointment saved = appointmentRepository.save(appointment);
                service.setTimesBooked(service.getTimesBooked() + 1);

                Map<String, Object> depositModel = new HashMap<>(base);
                depositModel.put("remainingAmount", remainingBalance);

                Map<String, Object> adminModel = new HashMap<>();
                String clientType = appointment.getUser() != null ? "Member" : "Guest";
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
                adminModel.put("adminAppointmentUrl", frontendProps.baseUrl() + "/admin/appointment/" + appointment.getId());

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
                payment.setPaymentStatus(PaymentStatus.PAID_IN_FULL_ACH);
                paymentRepository.save(payment);

                appointment.setAppointmentStatus(AppointmentStatus.COMPLETED);
                appointment.setPaymentStatus(PaymentStatus.PAID_IN_FULL_ACH);
                appointment.setRemainingBalance(BigDecimal.ZERO);

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
                finalModel.put("serviceAmount", appointment.getService().getPrice());
                finalModel.put("addOns", addOnLines);
                finalModel.put("subtotal", addOnsTotal.add(service.getPrice()));
                finalModel.put("discountAmount", BigDecimal.ZERO);
                finalModel.put("chargedToday", appointment.getTotalAmount().subtract(appointment.getDepositAmount()));
                finalModel.put("totalPaid", appointment.getTotalAmount());
                if (appointment.getTipAmount() != null) {
                    finalModel.put("tipAmount", appointment.getTipAmount());
                }

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
