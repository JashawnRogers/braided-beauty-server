package com.braided_beauty.braided_beauty.controllers;


import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import com.braided_beauty.braided_beauty.enums.PaymentStatus;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.repositories.AppointmentRepository;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.model.Event;
import com.stripe.exception.SignatureVerificationException;

import com.stripe.model.PaymentIntent;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhook/stripe")
public class StripeWebhookController {


    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final String webhookSecret;
    private final AppointmentRepository appointmentRepository;

    public StripeWebhookController(@Value("${stripe.webhook.secret}") String webhookSecret,
                                   AppointmentRepository appointmentRepository) {
        this.webhookSecret = webhookSecret;
        this.appointmentRepository = appointmentRepository;
    }

    private static final Set<String> ALLOWED_EVENTS = Set.of(
            "checkout.session.completed",      // confirms user completed checkout
            "payment_intent.succeeded",        // backup check — but session is better
            "payment_intent.payment_failed",   // payment failed
            "payment_intent.canceled",         // customer closed checkout before completing
            "checkout.session.expired"         // checkout session timed out
    );

    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody String payload,
                                                @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event;

        try{
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.badRequest().body("Invalid signature.");
        }

        if (!ALLOWED_EVENTS.contains(event.getType())) {
            log.info("Ignoring event type: {}", event.getType());
            // So that Stripe doesn't keep retrying to grab the event
            return ResponseEntity.ok("Event type ignored.");
        }

       switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutCompleted(event);
            case "payment_intent.payment_failed" -> handlePaymentFailed(event);
       }

        return ResponseEntity.ok("{received : true}");
    }

    private void handleCheckoutCompleted(Event event){
        Session session = (Session) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new NotFoundException("Checkout session not found."));

        String appointmentId = session.getMetadata().get("appointmentId");
        if (appointmentId == null) {
            log.warn("No appointmentId found in session metadata.");
            return;
        }

        appointmentRepository.findById(UUID.fromString(appointmentId))
                .ifPresent(appointment ->  {
                    if (appointment.getPaymentStatus() == PaymentStatus.SUCCEEDED) {
                        log.warn("Appointment {} already marked as paid. Skipping.", appointmentId);
                        return;
                    }

                    appointment.setAppointmentStatus(AppointmentStatus.COMPLETED);
                    appointment.setPaymentStatus(PaymentStatus.SUCCEEDED);
                    appointmentRepository.save(appointment);
                    log.info("Appointment {} marked as COMPLETED.", appointmentId);
                });
    }

    private void handlePaymentFailed(Event event){
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new NotFoundException("Payment intent not found."));

        String appointmentId = paymentIntent.getMetadata().get("appointmentId");

        if (appointmentId != null){
            appointmentRepository.findById(UUID.fromString(appointmentId))
                    .ifPresent(appointment -> {
                        appointment.setAppointmentStatus(AppointmentStatus.PAYMENT_FAILED);
                        appointment.setPaymentStatus(PaymentStatus.FAILED);
                        appointmentRepository.save(appointment);
                        log.info("Appointment {} marked as PAYMENT_FAILED.", appointmentId);
                    });
        } else {
            log.warn("No appointment ID found in metadata.");
        }
    }
}
