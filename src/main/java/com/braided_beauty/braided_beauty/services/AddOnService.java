package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.addOn.AddOnRequestDTO;
import com.braided_beauty.braided_beauty.dtos.addOn.AddOnResponseDTO;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.mappers.addOn.AddOnDTOMapper;
import com.braided_beauty.braided_beauty.models.AddOn;
import com.braided_beauty.braided_beauty.repositories.AddOnRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
@Getter
@Setter
public class AddOnService {
    private final AddOnRepository addOnRepository;
    private final AddOnDTOMapper addOnDTOMapper;

    public List<AddOn> getAddOnIds(List<UUID> ids) {
        return addOnRepository.findAllById(ids);

    }

    public Page<AddOnResponseDTO> getAllAddOns(
            String search,
            LocalDateTime createdAtFrom,
            LocalDateTime createdAtTo,
            Pageable pageable
    ) {
        Specification<AddOn> spec = (root, q, cb) -> cb.conjunction();

        if (search != null && !search.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("price")), "%" + search.toLowerCase() + "%")
            ));
        }

        if (createdAtFrom != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), createdAtFrom));
        }
        if (createdAtTo != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), createdAtTo));
        }

        return addOnRepository.findAll(spec, pageable)
                .map(addOnDTOMapper::toDto);
    }

    @Transactional
    public AddOnResponseDTO save(AddOnRequestDTO dto) {
        AddOn addOn;

        if (dto.getId() != null) {
            addOn = addOnRepository.findById(dto.getId())
                    .orElseThrow(() -> new NotFoundException("Add on not found: " + dto.getId()));
            addOnDTOMapper.update(addOn, dto);
        } else {
            addOn = addOnDTOMapper.create(dto);
        }

        addOnRepository.save(addOn);
        return addOnDTOMapper.toDto(addOn);
    }
}
