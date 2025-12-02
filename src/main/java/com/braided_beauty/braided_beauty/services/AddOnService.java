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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@AllArgsConstructor
@Getter
@Setter
public class AddOnService {
    private final AddOnRepository addOnRepository;
    private final AddOnDTOMapper addOnDTOMapper;

    public AddOnResponseDTO getAddOn(UUID id) {
        return addOnRepository.findById(id).map(addOnDTOMapper::toDto).orElseThrow(() ->
                new NotFoundException("No add on found with that ID: " + id));
    }

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
            Specification<AddOn> byName =
                    (root, q, cb) ->
                            cb.like(cb.lower(root.get("name")), "%" + search.trim().toLowerCase(Locale.ROOT) + "%");

            Specification<AddOn> byPrice = null;
            try {
                BigDecimal price = new BigDecimal(search.trim());
                byPrice = (root, q, cb) ->
                        cb.equal(root.get("price"), price);
            } catch (NumberFormatException ignored) {
                // not numeric -> skip price criterion
            }

            // Combine: (name LIKE ...) OR (price = ...)
            spec = spec.and(byPrice != null ? byName.or(byPrice) : byName);
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

    public List<AddOnResponseDTO> getAllAddOnsByList() {
        return addOnRepository.findAll()
                .stream()
                .map(addOnDTOMapper::toDto)
                .toList();
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
