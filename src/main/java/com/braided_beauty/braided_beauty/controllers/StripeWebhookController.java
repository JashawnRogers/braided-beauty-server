package com.braided_beauty.braided_beauty.controllers;



import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.services.PaymentService;
import com.stripe.model.checkout.Session;
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
            "checkout.session.completed",      // confirms user completed checkout
            "payment_intent.payment_failed"   // payment failed

    );

    @Operation(
            summary = "Stripe Webhook Endpoint",
            description = "Handles incoming webhook events from Stripe such as checkout completion and payment failures.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Event received and processed"),
            @ApiResponse(responseCode = "400", description = "Invalid signature or failed deserialization")
    })
    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody String payload,
                                                @Parameter(name = "Stripe-Signature", in = ParameterIn.HEADER, required = true, description = "Stripe signature header")
                                                @RequestHeader String sigHeader) {
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
