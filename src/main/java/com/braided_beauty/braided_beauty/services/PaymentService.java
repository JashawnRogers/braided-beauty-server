package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import com.braided_beauty.braided_beauty.enums.PaymentStatus;
import com.braided_beauty.braided_beauty.enums.PaymentType;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.models.Appointment;
import com.braided_beauty.braided_beauty.models.Payment;
import com.braided_beauty.braided_beauty.repositories.AppointmentRepository;
import com.braided_beauty.braided_beauty.repositories.PaymentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
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
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@AllArgsConstructor
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AppointmentRepository appointmentRepository;
    private final LoyaltyService loyaltyService;
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

        BigDecimal deposit = appointment.getDepositAmount() == null ? BigDecimal.ZERO : appointment.getDepositAmount();
        BigDecimal serviceAmount = appointment.getService().getPrice();
        BigDecimal remainingBalance = serviceAmount.subtract(deposit);
        BigDecimal finalAmount = remainingBalance.add(tipAmount);
        String email = appointment.getUser() != null ? appointment.getUser().getEmail() : appointment.getGuestEmail();

        if (remainingBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Deposit exceeds service price for appointment " + appointment.getId());
        }

        List<SessionCreateParams.LineItem> lineItems = Stream.of(
                // Remaining balance line item
                remainingBalance.compareTo(BigDecimal.ZERO) > 0 ?
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
                        .build()
                : null,

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
    public void handlePaymentIntentSucceeded(PaymentIntent paymentIntent) {
        String paymentType = paymentIntent.getMetadata().get("paymentType");
        String appointmentIdString = paymentIntent.getMetadata().get("appointmentId");

        if (paymentType == null || appointmentIdString == null) {
            log.warn("Missing metadata on PaymentIntent {}: appointmentId/paymentType", paymentIntent.getId());
            return;
        }

        Appointment appointment = appointmentRepository.findById(UUID.fromString(appointmentIdString))
                .orElseThrow(() -> new NotFoundException("Appointment not found: " + appointmentIdString));

        Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntent.getId())
                .orElseThrow(() -> new NotFoundException("Payment not found for appointment: " + appointmentIdString));

        if ((payment.getPaymentStatus() == PaymentStatus.PAID_IN_FULL_ACH && "final".equals(paymentType)) ||
                (payment.getPaymentStatus() == PaymentStatus.PAID_DEPOSIT && "deposit".equals(paymentType))
        ) {
            log.info("PaymentIntent {} already processed (status={})", paymentIntent.getId(), payment.getPaymentStatus());
            return;
        }

        if ("deposit".equals(paymentType)) {
            payment.setPaymentStatus(PaymentStatus.PAID_DEPOSIT);
            paymentRepository.save(payment);

            appointment.setAppointmentStatus(AppointmentStatus.CONFIRMED);
            appointment.setPaymentStatus(PaymentStatus.PAID_DEPOSIT);
            appointment.setHoldExpiresAt(null);
            appointmentRepository.save(appointment);

            log.info("Deposit completed via paymentIntent {} for appointment {}", paymentIntent.getId(), appointmentIdString);
        } else if ("final".equals(paymentType)) {
            payment.setPaymentStatus(PaymentStatus.PAID_IN_FULL_ACH);
            paymentRepository.save(payment);

            appointment.setAppointmentStatus(AppointmentStatus.COMPLETED);
            appointment.setPaymentStatus(PaymentStatus.PAID_IN_FULL_ACH);
            // award loyalty points with idempotent flag
            if (!appointment.isLoyaltyApplied()) {
                loyaltyService.awardForCompletedAppointment(appointment.getUser());
                appointment.setLoyaltyApplied(true);
            }
            appointmentRepository.save(appointment);

            log.info("Final payment completed via paymentIntent {} for appointment {}", paymentIntent.getId(), appointmentIdString);
        } else {
            log.warn("Unknown paymentType {} on paymentIntent {}", paymentType, paymentIntent.getId());
        }
    }

    @Transactional
    public void handlePaymentIntentFailed(PaymentIntent paymentIntent) {
        String appointmentIdString = paymentIntent.getMetadata().get("appointmentId");

        if (appointmentIdString == null) {
            log.warn("PaymentIntent missing appointmentId metadata. paymentIntent: {}", paymentIntent.getId());
            return;
        }

        UUID appointmentId = UUID.fromString(appointmentIdString);

        appointmentRepository.findById(appointmentId)
                .ifPresent(appointment -> {
                    appointment.setAppointmentStatus(AppointmentStatus.CANCELED);
                    appointment.setPaymentStatus(PaymentStatus.PAYMENT_FAILED);
                    appointment.setHoldExpiresAt(null);
                    appointmentRepository.save(appointment);
                    log.info("Appointment {} marked as PAYMENT_FAILED. / paymentIntent {}", appointmentId, paymentIntent.getId());
                });
    }
}
