package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.exceptions.BadRequestException;
import com.braided_beauty.braided_beauty.models.PromoCode;
import com.braided_beauty.braided_beauty.repositories.PromoCodeRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

@Service
@AllArgsConstructor
public class PromoCodeValidationService {
    private final PromoCodeRepository promoCodeRepository;
    private final Clock clock;

    public PromoCode validateAndGetOrNull(String rawPromoCode) {
        String code = normalize(rawPromoCode);

        if (code == null) {
            return null;
        }

        PromoCode promo = promoCodeRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new BadRequestException("Promo code is invalid."));

        if (!promo.isActive()) {
            throw new BadRequestException("Promo code is invalid.");
        }

        LocalDateTime now = LocalDateTime.now(clock);

        if (promo.getStartsAt() != null && now.isBefore(promo.getStartsAt())) {
            throw new BadRequestException("Promo code is not active yet.");
        }

        if (promo.getEndsAt() != null && now.isAfter(promo.getEndsAt())) {
            throw new BadRequestException("Promo code has expired.");
        }

        // May be null and null = unlimited
        Integer max = promo.getMaxRedemptions();

        int redeemed = Objects.requireNonNullElse(promo.getTimesRedeemed(), 0);

        if (max != null && redeemed >= max) {
            throw new BadRequestException("Promo code has reached its' redemption limit.");
        }

        validateDiscountDefinition(promo);

        return promo;
    }

    private void validateDiscountDefinition(PromoCode promo) {
        if (promo.getDiscountType() == null) {
            throw new BadRequestException("Promo code is misconfigured (missing discount type).");
        }

        if (promo.getValue() == null) {
            throw new BadRequestException("Promo code is misconfigured (missing value).");
        }

        BigDecimal value = promo.getValue();
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Promo code is misconfigured (negative value).");
        }

        switch (promo.getDiscountType()) {
            case PERCENT -> {
                if (value.compareTo(BigDecimal.ZERO) <= 0 || value.compareTo(new BigDecimal("100")) > 0) {
                    throw new BadRequestException("Promo code is misconfigured (percent must be 0–100).");
                }
            }
            case AMOUNT -> {
                if (value.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BadRequestException("Promo code is misconfigured (amount must be > 0).");
                }
            }
        }
    }

    private String normalize(String rawCode) {
        if (rawCode == null || rawCode.isEmpty()) return null;
        return rawCode.trim().toUpperCase(Locale.ROOT);
    }

}
