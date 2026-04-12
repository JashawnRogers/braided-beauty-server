package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.enums.UserType;
import com.braided_beauty.braided_beauty.exceptions.BadRequestException;
import com.braided_beauty.braided_beauty.exceptions.ConflictException;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.models.User;
import com.braided_beauty.braided_beauty.records.BootstrapAdminProperties;
import com.braided_beauty.braided_beauty.records.BootstrapAdminResponse;
import com.braided_beauty.braided_beauty.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BootstrapAdminService {
    private final UserRepository userRepository;
    private final BootstrapAdminProperties properties;
    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminService.class);

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BootstrapAdminResponse bootstrapCurrentUser(UUID userId, String providedSecret) {
        if (!properties.enabled()) {
            log.warn("Bootstrap admin request rejected because the flow is disabled. userId={}", userId);
            throw new BadRequestException("Bootstrap admin flow is disabled.");
        }

        if (userRepository.existsByUserType(UserType.ADMIN)) {
            log.warn("Bootstrap admin request rejected because an admin already exists. userId={}", userId);
            throw new ConflictException("Bootstrap admin flow is no longer available because an admin already exists.");
        }

        if (!matchesSecret(providedSecret)) {
            log.warn("Bootstrap admin request rejected because the secret was invalid. userId={}", userId);
            throw new BadRequestException("Bootstrap admin secret is missing or invalid.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Authenticated user was not found."));

        user.setUserType(UserType.ADMIN);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("Bootstrap admin promotion completed. userId={}", user.getId());

        return new BootstrapAdminResponse("Authenticated user promoted to ADMIN.");
    }

    private boolean matchesSecret(String providedSecret) {
        String expectedSecret = properties.secret();
        if (expectedSecret == null || expectedSecret.isBlank() || providedSecret == null || providedSecret.isBlank()) {
            return false;
        }

        return MessageDigest.isEqual(
                expectedSecret.getBytes(StandardCharsets.UTF_8),
                providedSecret.getBytes(StandardCharsets.UTF_8)
        );
    }
}
