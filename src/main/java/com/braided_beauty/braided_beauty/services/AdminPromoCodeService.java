package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import com.braided_beauty.braided_beauty.exceptions.BadRequestException;
import com.braided_beauty.braided_beauty.exceptions.ConflictException;
import com.braided_beauty.braided_beauty.exceptions.DuplicateEntityException;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.models.PromoCode;
import com.braided_beauty.braided_beauty.enums.DiscountType;
import com.braided_beauty.braided_beauty.records.PromoCodeDTO;
import com.braided_beauty.braided_beauty.repositories.AppointmentRepository;
import com.braided_beauty.braided_beauty.repositories.PromoCodeRepository;
import com.braided_beauty.braided_beauty.utils.PromoCodeSpecifications;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AdminPromoCodeService {
    private final PromoCodeRepository promoCodeRepository;
    private final AppointmentRepository appointmentRepository;


    private static final Integer DEFAULT_MAX_REDEMPTIONS = 9999;


    @Transactional
    public PromoCodeDTO createPromoCode(PromoCodeDTO dto) {
        if (dto == null) throw new IllegalArgumentException("PromoCode payload is required");

        String code = (dto.codeName() == null) ? null : dto.codeName().trim();
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Promo code name is required.");
        }
        code = code.toUpperCase(Locale.ROOT);

        PromoCode existingPromoCode = promoCodeRepository.findByCodeIgnoreCase(code)
                .orElse(null);

        if (existingPromoCode != null) {
            if (existingPromoCode.isActive()) {
                throw new DuplicateEntityException(
                        "Promo code with name already exists and cannot share the same name.");
            }

            PromoCodeDTO dtoFromExistingPromo = PromoCodeDTO.builder()
                    .id(existingPromoCode.getId())
                    .codeName(dto.codeName())
                    .discountType(dto.discountType())
                    .value(dto.value())
                    .active(dto.active())
                    .startsAt(dto.startsAt())
                    .endsAt(dto.endsAt())
                    .maxRedemptions(dto.maxRedemptions())
                    .timesRedeemed(0)
                    .build();

            return updatePromoCode(dtoFromExistingPromo, dtoFromExistingPromo.id());
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

        LocalDateTime startsAt = dto.startsAt();
        LocalDateTime endsAt = dto.endsAt();

        if (startsAt != null && endsAt.isBefore(startsAt) && endsAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Promo code end date cannot be before today or start date.");
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
            Throwable root = ex.getMostSpecificCause();
            String msg = root != null ? root.getMessage() : "";

            if (msg != null && msg.contains("uk_promo_code")) {
                throw new DuplicateEntityException("Promo codes cannot share the same name.");
            }

            throw new BadRequestException("Invalid promo code data.");
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

    @Transactional
    public PromoCodeDTO updatePromoCode(PromoCodeDTO dto, UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Promo code ID is required to update.");
        }
        if (dto == null) {
            throw new IllegalArgumentException("PromoCode payload is required.");
        }
        if (dto.id() == null) {
            throw new IllegalArgumentException("PromoCode ID is required.");
        }

        if (!id.equals(dto.id())) {
            throw new IllegalArgumentException("Path ID must match payload ID.");
        }

        PromoCode promoCode = promoCodeRepository.findById(dto.id())
                .orElseThrow(() -> new NotFoundException("Promo code not found."));

        if (dto.codeName() != null && !dto.codeName().isBlank()) {
            String newCodeName = dto.codeName().trim().toUpperCase(Locale.ROOT);

            if (!newCodeName.equalsIgnoreCase(promoCode.getCode())) {
                if (promoCodeRepository.existsByCodeIgnoreCase(newCodeName)) {
                    throw new DuplicateEntityException("Promo codes cannot share the same name.");
                }
                promoCode.setCode(newCodeName);
            }
        }

        if (dto.discountType() != null && dto.discountType() != promoCode.getDiscountType()) {
            promoCode.setDiscountType(dto.discountType());
        }

        if (dto.value() != null) {
            BigDecimal value = dto.value();

            if (value.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Promo codes cannot have a negative value.");
            }

            DiscountType discountType = promoCode.getDiscountType();
            if (discountType == null) {
                throw new IllegalArgumentException("Promo codes must have a discount type.");
            }

            if (promoCode.getValue() != null && value.compareTo(promoCode.getValue()) != 0) {
                switch (discountType) {
                    case PERCENT -> {
                        if (value.compareTo(new BigDecimal("100")) > 0) {
                            throw new IllegalArgumentException("Discount percentage cannot exceed 100%.");
                        }

                        if (value.compareTo(BigDecimal.ONE) < 0) {
                            throw new IllegalArgumentException("Discount percentage must be at least 1%.");
                        }
                        promoCode.setValue(value);
                    }
                    case AMOUNT -> promoCode.setValue(value);
                }
            }
        }

        if (dto.active() != null && dto.active() != promoCode.isActive()) {
            promoCode.setActive(dto.active());
        }

        LocalDateTime newStartsAt = (dto.startsAt() != null) ? dto.startsAt() : promoCode.getStartsAt();
        LocalDateTime newEndsAt = (dto.endsAt() != null) ? dto.endsAt() : promoCode.getEndsAt();

        if (newStartsAt != null && newEndsAt != null && newEndsAt.isBefore(newStartsAt)) {
            throw new IllegalArgumentException("Promo code end date cannot be before start date.");
        }

        if (dto.startsAt() != null
                && (promoCode.getStartsAt() == null || !dto.startsAt().equals(promoCode.getStartsAt()))
        ) {
            promoCode.setStartsAt(dto.startsAt());
        }

        if (dto.endsAt() != null
            && (promoCode.getEndsAt() == null || !dto.endsAt().equals(promoCode.getEndsAt()))
        ) {
            promoCode.setEndsAt(dto.endsAt());
        }

        if (dto.maxRedemptions() != null && !dto.maxRedemptions().equals(promoCode.getMaxRedemptions())) {
            if (dto.maxRedemptions() < 0) {
                throw new IllegalArgumentException("Max redemptions cannot be a negative number.");
            }
            promoCode.setMaxRedemptions(dto.maxRedemptions());
        }

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

    @Transactional
    public void softDelete(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Promo code ID is required to delete promo.");
        }

        PromoCode promoCode = promoCodeRepository.findById(id)
                        .orElseThrow(() -> new NotFoundException("Promo code not found"));

        boolean inUseByActiveAppointments =
                appointmentRepository.existsByPromoCode_IdAndAppointmentStatusIn(promoCode.getId(),
                        List.of(AppointmentStatus.PENDING_CONFIRMATION, AppointmentStatus.CONFIRMED));

        if (inUseByActiveAppointments) {
            throw new ConflictException(
                    "Cannot delete promo code because it is applied to active appointments. Deactivate it instead."
            );
        }

        if (promoCode.isActive()) {
            promoCode.setActive(false);
            promoCodeRepository.save(promoCode);
        }
    }

    @Transactional(readOnly = true)
    public Page<PromoCodeDTO> listPromoCodes(String q, Boolean active, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Specification<PromoCode> spec = Specification.allOf(
                PromoCodeSpecifications.codeContainsIgnoreCase(q),
                PromoCodeSpecifications.hasActive(active)
        );


        return promoCodeRepository.findAll(spec, pageable)
                .map(this::toDto);
    }

    public PromoCodeDTO getPromoCode(UUID id) {
        if (id == null) {
            throw new BadRequestException("Must send ID to retrieve promo code.");
        }

        PromoCode promoCode = promoCodeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promo code not found."));

        return toDto(promoCode);
    }

    private PromoCodeDTO toDto(PromoCode p) {
        return PromoCodeDTO.builder()
                .id(p.getId())
                .codeName(p.getCode())
                .discountType(p.getDiscountType())
                .value(p.getValue())
                .active(p.isActive())
                .startsAt(p.getStartsAt())
                .endsAt(p.getEndsAt())
                .maxRedemptions(p.getMaxRedemptions())
                .timesRedeemed(p.getTimesRedeemed())
                .build();
    }
}
