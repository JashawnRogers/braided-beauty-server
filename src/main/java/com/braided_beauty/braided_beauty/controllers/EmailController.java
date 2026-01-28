package com.braided_beauty.braided_beauty.controllers;

import com.braided_beauty.braided_beauty.records.EmailRequest;
import com.braided_beauty.braided_beauty.services.EmailService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/email")
@AllArgsConstructor
public class EmailController {
    private final EmailService service;

    @PostMapping("/send-html")
    public ResponseEntity<String> sendHtmlEmail(@RequestBody EmailRequest request) {
        try {
            service.sendHtmlEmail(request.to(), request.subject(), request.html());
            return ResponseEntity.ok("HTML Email sent successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failure to send email. " + e.getMessage());
        }
    }
}
