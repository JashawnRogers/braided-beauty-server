package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.shared.BusinessHoursRequestDTO;
import com.braided_beauty.braided_beauty.dtos.shared.BusinessHoursResponseDTO;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.mappers.businessHours.BusinessHoursDtoMapper;
import com.braided_beauty.braided_beauty.models.BusinessHours;
import com.braided_beauty.braided_beauty.repositories.BusinessHoursRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(BusinessHoursService.class);

    @Transactional
    public BusinessHoursResponseDTO create(BusinessHoursRequestDTO dto){
        validate(dto);
        if (repo.existsByDayOfWeek(dto.getDayOfWeek())) {
            throw new IllegalArgumentException("Hours for " + dto.getDayOfWeek() + " already exists.");
        }
        BusinessHours created = mapper.toEntity(dto);
        repo.save(created);
        log.info("Created new business hours entity. ID: {}", created.getId());
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
        log.info("Updated business hours entity, ID: {}", entity.getId());
        return mapper.toDTO(entity);
    }

    @Transactional
    public void delete(UUID id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Business hours not found: " + id);
        }
        log.info("Delete business hours entity ID: {}", id);
        repo.deleteById(id);
    }

    public BusinessHoursResponseDTO getOne(UUID id) {
        BusinessHours entity = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Business hours not found: " + id));
        log.info("Returning business hours entity, ID: {}", id);
        return mapper.toDTO(entity);
    }

    public List<BusinessHoursResponseDTO> getAll() {
        log.info("Returning all business hours");
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

