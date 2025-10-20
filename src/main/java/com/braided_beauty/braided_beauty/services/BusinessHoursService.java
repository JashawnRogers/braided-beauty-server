package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.shared.BusinessHoursRequestDTO;
import com.braided_beauty.braided_beauty.dtos.shared.BusinessHoursResponseDTO;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.mappers.businessHours.BusinessHoursDtoMapper;
import com.braided_beauty.braided_beauty.models.BusinessHours;
import com.braided_beauty.braided_beauty.repositories.BusinessHoursRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
@Getter
public class BusinessHoursService {
    private final BusinessHoursRepository repo;
    private final BusinessHoursDtoMapper mapper;

    @Transactional
    public BusinessHoursResponseDTO create(BusinessHoursRequestDTO dto){
        validate(dto);
        if (repo.existsByDayOfWeek(dto.getDayOfWeek())) {
            throw new IllegalArgumentException("Hours for " + dto.getDayOfWeek() + " already exists.");
        }
        BusinessHours created = mapper.toEntity(dto);
        repo.save(created);
        return mapper.toDTO(created);
    }

    @Transactional
    public BusinessHoursResponseDTO update(UUID id, BusinessHoursRequestDTO dto) {
        validate(dto);

        BusinessHours entity = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Business hours not found: " + id));

        if (!entity.getDayOfWeek().equals(dto.getDayOfWeek())
        &&  repo.existsByDayOfWeekAndIdNot(dto.getDayOfWeek(), id)) {
            throw new IllegalArgumentException("Hours for " + dto.getDayOfWeek() + " already exist.");
        }

        mapper.update(entity, dto);

        if (entity.isClosed()) {
            entity.setOpenTime(null);
            entity.setCloseTime(null);
        }

        entity = repo.save(entity);
        return mapper.toDTO(entity);
    }

    @Transactional
    public void delete(UUID id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Business hours not found: " + id);
        }
        repo.deleteById(id);
    }

    public BusinessHoursResponseDTO get(UUID id) {
        BusinessHours entity = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Business hours not found: " + id));
        return mapper.toDTO(entity);
    }

    public List<BusinessHoursResponseDTO> getAll() {
        return repo.findAll()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    private void validate(BusinessHoursRequestDTO dto) {
        if (dto.isClosed()) {
            if (dto.getOpenTime() != null || dto.getCloseTime() != null) {
                throw new IllegalArgumentException("Closed days must not have open/close times");
            }
            return;
        }
        // open -> must HAVE both times
        if (dto.getOpenTime() == null || dto.getCloseTime() == null) {
            throw new IllegalArgumentException("Open days must include both openTime and closeTime");
        }
        if (!dto.getOpenTime().isBefore(dto.getCloseTime())) {
            throw new IllegalArgumentException("openTime must be before closeTime");
        }
        }
    }

