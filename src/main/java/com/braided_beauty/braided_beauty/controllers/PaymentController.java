package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.services.PaymentService;
import com.stripe.exception.StripeException;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@AllArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

}
