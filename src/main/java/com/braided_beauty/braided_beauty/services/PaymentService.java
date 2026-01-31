package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import com.braided_beauty.braided_beauty.enums.PaymentStatus;
import com.braided_beauty.braided_beauty.enums.PaymentType;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.models.*;
import com.braided_beauty.braided_beauty.records.EmailAddOnLine;
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

import java.lang.String;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
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

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Transactional
    public void updateRefundPayment(Appointment appointment) throws StripeException {
        Payment payment = paymentRepository.findByAppointmentIdAndPaymentType(appointment.getId(), PaymentType.FINAL)
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

        String email = appointment.getUser().getEmail() != null ? appointment.getUser().getEmail() : appointment.getGuestEmail();
        BusinessSettings settings = businessSettingsService.getOrCreate();
        String companyPhoneNumber  = PhoneNormalizer.formatForEmail(settings.getCompanyPhoneNumber());

        log.info("Payment Type: {}", paymentType);
        if ("deposit".equals(paymentType) && appointment.getPaymentStatus() == PaymentStatus.PAID_DEPOSIT) {
            log.info("Deposit already processed for appointment {}", appointmentId);
            return;
        }

        Payment payment = paymentRepository.findByStripeSessionId(session.getId())
                .orElseThrow(() -> new NotFoundException("Payment not found for session: " + session.getId()));

        List<EmailAddOnLine> addOnLines = appointment.getAddOns().stream()
                .map(a -> new EmailAddOnLine(a.getName(), a.getPrice().toString()))
                .toList();

        Map<String, Object> base = new HashMap<>();
        base.put("customerName", appointment.getUser().getName() != null ? appointment.getUser().getName() : "Guest");
        base.put("appointmentDateTime", appointment.getAppointmentTime());
        base.put("serviceName", appointment.getService().getName());
        base.put("depositAmount", appointment.getDepositAmount());
        base.put("businessPhone", companyPhoneNumber);
        base.put("businessAddress",settings.getCompanyAddress());

        try {
            switch (payment.getPaymentType()) {
                case DEPOSIT ->  {
                    payment.setPaymentStatus(PaymentStatus.PAID_DEPOSIT);
                    paymentRepository.save(payment);
                    appointment.setStripeSessionId(session.getId());
                    appointment.setAppointmentStatus(AppointmentStatus.CONFIRMED);
                    appointment.setPaymentStatus(PaymentStatus.PAID_DEPOSIT);
                    appointment.setHoldExpiresAt(null);
                    appointment.setRemainingBalance(remainingBalance);
                    appointment.setLoyaltyApplied(false);
                    Appointment saved = appointmentRepository.save(appointment);

                    Map<String, Object> depositModel = new HashMap<>(base);
                    depositModel.put("remainingAmount", remainingBalance);

                    String html = emailTemplateService.render("deposit-receipt", depositModel);
                    emailService.sendHtmlEmail(email, "Deposit confirmation", html);

                    appointmentConfirmationService.ensureConfirmationTokenForAppointment(saved.getId());
                    log.info("Deposit completed via checkout session {} for appointment {}", session.getId(), appointmentId);
                    return;
                }
                case FINAL -> {
                    payment.setPaymentStatus(PaymentStatus.PAID_IN_FULL_ACH);
                    paymentRepository.save(payment);

                    appointment.setAppointmentStatus(AppointmentStatus.COMPLETED);
                    appointment.setPaymentStatus(PaymentStatus.PAID_IN_FULL_ACH);
                    appointment.setRemainingBalance(BigDecimal.ZERO);

                    if (appointment.getUser() == null) {
                        log.info("Skipping loyalty reward (guest appointment {}", appointmentId);
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

                    String html = emailTemplateService.render("final-payment-receipt", finalModel);
                    emailService.sendHtmlEmail(email, "Final Payment Received", html);

                    log.info("Final payment completed via session: {} for appointment: {}", session.getId(), appointmentId);
                    return;
                }
            }
        } catch (Exception e) {
            log.error("FINAL webhook processing failed. sessionId={} apptId={} msg={}",
                    session.getId(), appointmentId, e.getMessage(), e);
            throw e;
        }

        log.warn("Unknown paymentType {} on paymentIntent {}", paymentType, session.getPaymentIntent());
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
        appointment.setHoldExpiresAt(null);
        appointmentRepository.save(appointment);
        log.info("Appointment {} marked as PAYMENT_FAILED. / paymentIntent {}", appointmentId, session.getPaymentIntent());
    }
}
