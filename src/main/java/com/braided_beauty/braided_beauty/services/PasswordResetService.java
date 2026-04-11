package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.models.PasswordResetToken;
import com.braided_beauty.braided_beauty.models.User;
import com.braided_beauty.braided_beauty.records.ForgotPasswordRequest;
import com.braided_beauty.braided_beauty.repositories.PasswordResetTokenRepository;
import com.braided_beauty.braided_beauty.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
public class PasswordResetService {
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;
    private final PasswordEncoder passwordEncoder;
    private final String frontendUrl;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            EmailService emailService,
            EmailTemplateService emailTemplateService,
            PasswordEncoder passwordEncoder,
            @Value("${app.frontend.base-url}") String frontendUrl
    ) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.emailTemplateService = emailTemplateService;
        this.passwordEncoder = passwordEncoder;
        this.frontendUrl = frontendUrl;
    }

    /**
     * Creates a single active reset token for the user and emails the frontend reset link.
     */
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest dto) {
        User user = userRepository.findUserByEmail(dto.email())
                .orElse(null);

        // Missing users are ignored to avoid disclosing which emails exist.
        if (user == null) {
            return;
        }

        passwordResetTokenRepository.invalidateUnusedTokensForUser(user.getId());

        String rawToken = UUID.randomUUID().toString() + UUID.randomUUID();
        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(tokenHash);
        resetToken.setCreatedAt(LocalDateTime.now());
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        resetToken.setUsed(false);

        passwordResetTokenRepository.save(resetToken);

        String resetLink = frontendUrl + "/reset-password?token=" + rawToken;

        String html = emailTemplateService.render("password-reset", Map.of(
                "name", user.getName(),
                "resetLink", resetLink
        ));

        emailService.sendHtmlEmail(
                user.getEmail(),
                "Reset Your Braided Beauty Password",
                html
        );

    }

    /**
     * Consumes a valid reset token and replaces the stored password hash.
     */
    @Transactional
    public void resetPassword(String rawToken, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findValidToken(tokenHash, LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token."));

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));

        resetToken.setUsed(true);
        resetToken.setUsedAt(LocalDateTime.now());

        passwordResetTokenRepository.save(resetToken);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to hash token", e);
        }
    }
}
