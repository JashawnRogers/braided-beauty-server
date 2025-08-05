package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import com.braided_beauty.braided_beauty.enums.PaymentStatus;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.mappers.payment.PaymentDtoMapper;
import com.braided_beauty.braided_beauty.models.Appointment;
import com.braided_beauty.braided_beauty.models.Payment;
import com.braided_beauty.braided_beauty.repositories.AppointmentRepository;
import com.braided_beauty.braided_beauty.repositories.PaymentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
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
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    public void updateRefundPayment(Appointment appointment) throws StripeException {
        Payment payment = paymentRepository.findByAppointmentId(appointment.getId())
                .orElseThrow(() -> new NotFoundException("No payment found for appointment."));

        RefundCreateParams refundParams = RefundCreateParams.builder()
                .setPaymentIntent(payment.getStripePaymentIntentId())
                .build();

        Refund.create(refundParams);

        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        log.info("Refund successful for appointment ID: {}", appointment.getId());
    }

    // Session are created to be sent to webhook
    public Session createDepositCheckoutSession(Appointment appointment, String successUrl, String cancelUrl) throws StripeException {
        BigDecimal deposit = appointment.getDepositAmount();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .setCustomerEmail(appointment.getUser().getEmail())
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
                                .build())
                .putMetadata("appointmentId", appointment.getId().toString())
                .putMetadata("userId", appointment.getUser() != null ? appointment.getUser().getId().toString() : "guest")
                .putMetadata("addOnIds", appointment.getAddOns().toString())
                .build();

        Session session = Session.create(params);

        Payment payment = Payment.builder()
                .stripeSessionId(session.getId())
                .amount(deposit)
                .paymentStatus(PaymentStatus.PENDING)
                .appointment(appointment)
                .user(appointment.getUser())
                .build();

        paymentRepository.save(payment);
        log.info("Created Stripe checkout session for deposit. Session ID: {}", session.getId());

        return session;
    }

    public Session createFinalPaymentSession(Appointment appointment, String successUrl, String cancelUrl, BigDecimal tipAmount) throws StripeException {
        if (tipAmount == null) {
            tipAmount = BigDecimal.ZERO;
        }

        BigDecimal serviceAmount = appointment.getService().getPrice();
        BigDecimal finalAmount = serviceAmount.add(tipAmount).subtract(appointment.getDepositAmount());

        List<SessionCreateParams.LineItem> lineItems = Stream.of(
                // Service amount line item
                SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(
                                SessionCreateParams.LineItem.PriceData.builder()
                                        .setCurrency("usd")
                                        .setUnitAmount(serviceAmount.movePointRight(2).longValue())
                                        .setProductData(
                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                        .setName("Service: " + appointment.getService().getName())
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
                                                .setUnitAmount(tipAmount.movePointRight(2).longValue())
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
                .setCustomerEmail(appointment.getUser().getEmail())
                .addAllLineItem(lineItems)
                .putMetadata("appointmentId", appointment.getId().toString())
                .putMetadata("userId", appointment.getUser().getId().toString())
                .putMetadata("tipAmount", tipAmount.toString())
                .putMetadata("paymentType", "final")
                .build();

        Session session = Session.create(params);

        Payment payment = Payment.builder()
                .stripeSessionId(session.getId())
                .amount(finalAmount)
                .tipAmount(tipAmount)
                .paymentStatus(PaymentStatus.PENDING)
                .appointment(appointment)
                .user(appointment.getUser())
                .build();

        paymentRepository.save(payment);
        log.info("Created Stripe checkout session for final payment. Session ID: {}", session.getId());

        return session;
    }

    // Called by webhook endpoint and is triggered after payment completes/fails
    public void handleFinalCheckoutCompleted(Session session){
        String appointmentIdString = session.getMetadata().get("appointmentId");
        String tipString = session.getMetadata().get("tipAmount");

        UUID appointmentId = UUID.fromString(appointmentIdString);
        BigDecimal tipAmount = tipString != null ? new BigDecimal(tipString) : BigDecimal.ZERO;

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found for final payment."));

        Payment payment = paymentRepository.findByStripeSessionId(session.getId())
                .orElseThrow(() -> new NotFoundException("Payment not found for final payment."));

        payment.setPaymentStatus(PaymentStatus.COMPLETED);
        payment.setTipAmount(tipAmount);
        paymentRepository.save(payment);

        appointment.setTipAmount(tipAmount);
        appointment.setPaymentStatus(PaymentStatus.COMPLETED);
        appointment.setAppointmentStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);

        log.info("Final payment completed for appointment {}", appointmentId);
    }

    public void handleDepositCheckoutCompleted(Session session) {
        String appointmentIdString = session.getMetadata().get("appointmentId");
        String userIdString = session.getMetadata().get("userId");

        if (appointmentIdString == null) {
            log.warn("Missing appointmentId metadata in deposit session");
            return;
        }

        if (userIdString == null) {
            log.warn("Missing userId metadata in deposit session");
            return;
        }

        UUID appointmentId = UUID.fromString(appointmentIdString);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found for deposit."));

        Payment payment = paymentRepository.findByStripeSessionId(session.getId())
                .orElseThrow(() -> new NotFoundException("Payment not found for deposit"));

        payment.setPaymentStatus(PaymentStatus.PAID_DEPOSIT);
        paymentRepository.save(payment);

        appointment.setAppointmentStatus(AppointmentStatus.CONFIRMED);
        appointment.setPaymentStatus(PaymentStatus.PAID_DEPOSIT);
        appointmentRepository.save(appointment);

        log.info("Deposit marked as paid for appointment {}", appointmentId);
    }

    public void handlePaymentFailed(Session session) {
        String appointmentIdString = session.getMetadata().get("appointmentId");

        if (appointmentIdString == null) {
            log.warn("No appointment ID found in failed payment session.");
            return;
        }

        UUID appointmentId = UUID.fromString(appointmentIdString);

        appointmentRepository.findById(appointmentId)
                .ifPresent(appointment -> {
                    appointment.setAppointmentStatus(AppointmentStatus.PAYMENT_FAILED);
                    appointment.setPaymentStatus(PaymentStatus.FAILED);
                    appointmentRepository.save(appointment);
                    log.info("Appointment {} marked as PAYMENT_FAILED.", appointmentId);
                });
    }
}
