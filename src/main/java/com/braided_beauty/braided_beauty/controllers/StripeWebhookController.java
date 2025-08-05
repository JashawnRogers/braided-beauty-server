package com.braided_beauty.braided_beauty.controllers;


import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import com.braided_beauty.braided_beauty.enums.PaymentStatus;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.models.Appointment;
import com.braided_beauty.braided_beauty.models.Payment;
import com.braided_beauty.braided_beauty.repositories.AppointmentRepository;
import com.braided_beauty.braided_beauty.repositories.PaymentRepository;
import com.braided_beauty.braided_beauty.services.PaymentService;
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

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhook/stripe")
public class StripeWebhookController {


    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final String webhookSecret;
    private final PaymentService paymentService;

    public StripeWebhookController(@Value("${stripe.webhook.secret}") String webhookSecret,
                                   PaymentService paymentService) {
        this.webhookSecret = webhookSecret;
        this.paymentService = paymentService;
    }

    private static final Set<String> ALLOWED_EVENTS = Set.of(
            "checkout.session.completed",      // confirms user completed checkout
            "payment_intent.payment_failed"   // payment failed

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
          case "checkout.session.completed" -> {
              Session session = (Session) event.getDataObjectDeserializer()
                      .getObject()
                      .orElseThrow(() -> new IllegalArgumentException("Failed to deserialize session object."));

              String paymentType = session.getMetadata().get("paymentType");

              if ("deposit".equals(paymentType)){
                  paymentService.handleDepositCheckoutCompleted(session);
                  log.info("Deposit success.");
              } else if ("final".equals(paymentType)) {
                  paymentService.handleFinalCheckoutCompleted(session);
                  log.info("Final payment success");
              }
          }
          case "payment_intent.payment_failed" -> {
              Session session = (Session) event.getDataObjectDeserializer()
                              .getObject()
                                      .orElseThrow(() -> new NotFoundException("Session object not found."));
              paymentService.handlePaymentFailed(session);
              log.warn("Payment failed: {}", session);
          }
       }

        return ResponseEntity.ok("{\"received\" : true}");
    }
}
