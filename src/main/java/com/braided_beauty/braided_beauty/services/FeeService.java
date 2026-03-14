package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.exceptions.DuplicateEntityException;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.models.Fee;
import com.braided_beauty.braided_beauty.records.CreateFeeDTO;
import com.braided_beauty.braided_beauty.records.FeeRequestDTO;
import com.braided_beauty.braided_beauty.records.FeeResponseDTO;
import com.braided_beauty.braided_beauty.repositories.AppointmentRepository;
import com.braided_beauty.braided_beauty.repositories.FeeRepository;
import com.braided_beauty.braided_beauty.utils.SearchSpecifications;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.UUID;

@Service
@AllArgsConstructor
public class FeeService {
    private final FeeRepository feeRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional
    public FeeResponseDTO createFee(CreateFeeDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("No data was passed.");
        }

        if (dto.name() == null || dto.name().isBlank()) {
            throw new IllegalArgumentException("Fee must contain a name.");
        }
        String name = dto.name();

        if (feeRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateEntityException("A fee with that name already exists.");
        }

        if (name.length() >= 100) {
            throw new IllegalArgumentException("Fee name must be no more than 100 characters");
        }


        if (dto.amount() == null) {
            throw new IllegalArgumentException("Fee amount must have a value.");
        }
        BigDecimal amount = money(dto.amount());

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be $0.00 or greater.");
        }

        Fee fee = Fee.builder()
                .name(name)
                .amount(amount)
                .build();

        Fee saved = feeRepository.save(fee);

        return toFeeResponseDTO(saved);
    }

    @Transactional
    public FeeResponseDTO updateFee(FeeRequestDTO dto, UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Fee ID was not passed as path variable.");
        }
        if (dto == null) {
            throw new IllegalArgumentException("No data was passed.");
        }

        Fee fee = feeRepository.findById(dto.id())
                .orElseThrow(() -> new NotFoundException("Fee not found."));

        String existingName = fee.getName().toLowerCase(Locale.ROOT);
        BigDecimal existingAmount = fee.getAmount().setScale(2, RoundingMode.HALF_UP);

        if (dto.name() != null && !dto.name().toLowerCase(Locale.ROOT).equals(existingName)) {
            fee.setName(dto.name().toLowerCase(Locale.ROOT));
        }

        if (dto.amount() != null) {
            BigDecimal delta = money(dto.amount().subtract(existingAmount));
            fee.setAmount(delta);
        }

        Fee saved = feeRepository.save(fee);

        return toFeeResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public FeeResponseDTO getFee(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Fee ID required to fetch fee.");
        }

        Fee fee = feeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Fee not found."));

        return toFeeResponseDTO(fee);
    }

    @Transactional(readOnly = true)
    public Page<FeeResponseDTO> listFees(String q, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Specification<Fee> spec = Specification.allOf(
                SearchSpecifications.codeContainsIgnoreCase(q, "name"),
                SearchSpecifications.hasActive(true, "active")
        );

        return feeRepository.findAll(spec, pageable)
                .map(this::toFeeResponseDTO);
    }

    @Transactional
    public void softDelete(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Fee ID is required to delete fee.");
        }

        Fee fee = feeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Fee not found."));

       fee.setActive(false);

       feeRepository.save(fee);
    }

    private BigDecimal money(BigDecimal value) {
        if (value == null) return null;
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private FeeResponseDTO toFeeResponseDTO(Fee fee) {
        return new FeeResponseDTO(
                fee.getId(),
                fee.getName(),
                money(fee.getAmount()),
                fee.getCreatedAt(),
                fee.getUpdatedAt(),
                fee.isActive()
        );
    }
}
