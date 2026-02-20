package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.exceptions.DuplicateEntityException;
import com.braided_beauty.braided_beauty.models.PromoCode;
import com.braided_beauty.braided_beauty.records.PromoCodeDTO;
import com.braided_beauty.braided_beauty.repositories.PromoCodeRepository;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@AllArgsConstructor
public class AdminPromoCodeService {
    private final PromoCodeRepository promoCodeRepository;

    private static final LocalDateTime DEFAULT_STARTS_AT_DATE = LocalDateTime.MIN;
    private static final LocalDateTime DEFAULT_ENDS_AT_DATE = LocalDateTime.MAX;
    private static final Integer DEFAULT_MAX_REDEMPTIONS = 9999;


    @Transactional
    public PromoCodeDTO createPromoCode(PromoCodeDTO dto) {
        if (dto == null) throw new IllegalArgumentException("PromoCode payload is required");

        String code = (dto.codeName() == null) ? null : dto.codeName().trim();
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Promo code name is required.");
        }
        code = code.toUpperCase(Locale.ROOT);

        if (promoCodeRepository.existsByCodeIgnoreCase(code)) {
            throw new DuplicateEntityException("Promo codes cannot share the same name.");
        }

        if (dto.discountType() == null) {
            throw new IllegalArgumentException("Promo codes must have a discount type.");
        }

        if (dto.value() == null) {
            throw new IllegalArgumentException("Promo codes must have a value.");
        }
        BigDecimal value = dto.value();
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Promo codes cannot have a negative value.");
        }

        switch (dto.discountType()) {
            case PERCENT -> {
                if (value.compareTo(new BigDecimal("100")) > 0) {
                    throw new IllegalArgumentException("Discount percentage cannot exceed 100%.");
                }

                if (value.compareTo(BigDecimal.ONE) < 0) {
                    throw new IllegalArgumentException("Discount percentage must be at least 1%.");
                }
            }
            case AMOUNT -> {}
        }

        LocalDateTime startsAt = (dto.startsAt() == null) ? DEFAULT_STARTS_AT_DATE : dto.startsAt();
        LocalDateTime endsAt = (dto.endsAt() == null) ? DEFAULT_ENDS_AT_DATE : dto.endsAt();

        if (endsAt.isBefore(startsAt)) {
            throw new IllegalArgumentException("Promo code end date cannot be before start date.");
        }

        Integer maxRedemptions = (dto.maxRedemptions() == null) ? DEFAULT_MAX_REDEMPTIONS : dto.maxRedemptions();
        if (maxRedemptions < 0) {
            throw new IllegalArgumentException("Max redemptions cannot be a negative number.");
        }

        PromoCode promoCode = PromoCode.builder()
                .code(code)
                .discountType(dto.discountType())
                .value(value)
                .active(dto.active())
                .startsAt(startsAt)
                .endsAt(endsAt)
                .maxRedemptions(maxRedemptions)
                .timesRedeemed(0)
                .build();

        PromoCode saved;
        try {
            saved = promoCodeRepository.save(promoCode);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateEntityException("Promo codes cannot share the same name.");
        }

        return PromoCodeDTO.builder()
                .id(saved.getId())
                .codeName(saved.getCode())
                .discountType(saved.getDiscountType())
                .value(saved.getValue())
                .active(saved.isActive())
                .startsAt(saved.getStartsAt())
                .endsAt(saved.getEndsAt())
                .maxRedemptions(saved.getMaxRedemptions())
                .timesRedeemed(saved.getTimesRedeemed())
                .build();
    }
}