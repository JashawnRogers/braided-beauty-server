package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.dtos.payment.PaymentIntentRequestDTO;
import com.braided_beauty.braided_beauty.dtos.payment.PaymentIntentResponseDTO;
import com.braided_beauty.braided_beauty.services.PaymentService;
import com.stripe.exception.StripeException;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@AllArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/create-intent")
    public ResponseEntity<PaymentIntentResponseDTO> createIntent(@RequestBody PaymentIntentRequestDTO dto){
        try{
            PaymentIntentResponseDTO response = paymentService.createPaymentIntent(dto);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            return ResponseEntity.status(500).build();
        }
    }
}
