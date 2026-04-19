package com.braided_beauty.braided_beauty.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    @Value("${app.mail.from-email:braidedbeautybrand@gmail.com}")
    private String fromEmail;
    @Value("${app.mail.from-name:Braided Beauty}")
    private String fromName;
    @Value("${app.mail.reply-to:braidedbeautybrand@gmail.com}")
    private String replyTo;

    public void sendHtmlEmail(String to, String subject, String html) {
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage,true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setReplyTo(replyTo);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }
}
