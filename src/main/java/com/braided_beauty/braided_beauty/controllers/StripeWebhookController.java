package com.braided_beauty.braided_beauty.controllers;



import com.braided_beauty.braided_beauty.records.StripeProperties;
import com.braided_beauty.braided_beauty.services.PaymentService;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.model.Event;
import com.stripe.exception.SignatureVerificationException;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/webhook/stripe")
@EnableConfigurationProperties(StripeProperties.class)
public class StripeWebhookController {


    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final String webhookSecret;
    private final PaymentService paymentService;

    public StripeWebhookController(StripeProperties props,
                                   PaymentService paymentService) {
        this.webhookSecret = props.webhookSecret();
        this.paymentService = paymentService;
    }

    private static final Set<String> ALLOWED_EVENTS = Set.of(
            "checkout.session.completed",
            "checkout.session.async_payment_succeeded",
            "checkout.session.async_payment_failed"
    );

    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody String payload,
                                                @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event;
        log.warn(">>>> STRIPE WEBHOOK HIT <<<<");
        try{
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.badRequest().body("Invalid signature.");
        }

        // So that Stripe doesn't keep retrying to grab the event
        if (!ALLOWED_EVENTS.contains(event.getType())) {
            log.info("Ignoring event type: {}", event.getType());
            return ResponseEntity.ok("Event type ignored.");
        }
        log.info("Event type: {}", event.getType());
       switch (event.getType()) {
            case "checkout.session.completed", "checkout.session.async_payment_succeeded" -> {
                Session session = (Session) event.getDataObjectDeserializer()
                        .getObject()
                        .orElseThrow(() -> new IllegalArgumentException("Failed to deserialize Session"));

                paymentService.handleCheckoutSessionCompleted(session);
                log.info("Checkout session completed: {}", session.getId());
            }
          case "checkout.session.async_payment_failed" -> {
              Session session = (Session) event.getDataObjectDeserializer()
                      .getObject()
                      .orElseThrow(() -> new IllegalArgumentException("Failed to deserialize Session"));

              paymentService.handleCheckoutSessionFailed(session);
              log.warn("Async payment failed. Session={}", session.getId());
          }
          default -> log.info("Unhandled event type: {}", event.getType());

       }

        return ResponseEntity.ok("{\"received\" : true}");
    }
}
