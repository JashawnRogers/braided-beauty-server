package com.braided_beauty.braided_beauty.controllers;



import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.services.PaymentService;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.model.Event;
import com.stripe.exception.SignatureVerificationException;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

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
            "payment_intent.payment_failed",
            "payment_intent.succeeded"

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

        // So that Stripe doesn't keep retrying to grab the event
        if (!ALLOWED_EVENTS.contains(event.getType())) {
            log.info("Ignoring event type: {}", event.getType());
            return ResponseEntity.ok("Event type ignored.");
        }

       switch (event.getType()) {
          case "payment_intent.succeeded" -> {
              PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                      .getObject()
                      .orElseThrow(() -> new IllegalArgumentException("Failed to deserialize PaymentIntent."));

              String paymentType = paymentIntent.getMetadata().get("paymentType");

              if ("deposit".equals(paymentType)){
                  paymentService.handlePaymentIntentSucceeded(paymentIntent);
                  log.info("Deposit success. paymentIntent: {}", paymentIntent.getId());
              } else if ("final".equals(paymentType)) {
                  paymentService.handlePaymentIntentSucceeded(paymentIntent);
                  log.info("Final payment success. paymentIntent: {}", paymentIntent.getId());
              }
          }
          case "payment_intent.payment_failed" -> {
              PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                      .getObject()
                      .orElseThrow(() -> new NotFoundException("Payment object not found."));
              paymentService.handlePaymentIntentFailed(paymentIntent);
              log.warn("Payment failed: {}", paymentIntent.getId());
          }
          default -> log.info("Unhandled event type: {}", event.getType());

       }

        return ResponseEntity.ok("{\"received\" : true}");
    }
}
