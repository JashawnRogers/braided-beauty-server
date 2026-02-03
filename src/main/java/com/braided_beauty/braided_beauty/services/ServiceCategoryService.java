package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.serviceCategory.ServiceCategoryCreateDTO;
import com.braided_beauty.braided_beauty.dtos.serviceCategory.ServiceCategoryUpdateDTO;
import com.braided_beauty.braided_beauty.dtos.serviceCategory.ServiceCategoryResponseDTO;
import com.braided_beauty.braided_beauty.exceptions.ConflictException;
import com.braided_beauty.braided_beauty.exceptions.DuplicateEntityException;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.mappers.serviceCategory.ServiceCategoryDtoMapper;
import com.braided_beauty.braided_beauty.models.ServiceCategory;
import com.braided_beauty.braided_beauty.repositories.ServiceCategoryRepository;
import com.braided_beauty.braided_beauty.repositories.ServiceRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@AllArgsConstructor
@Getter
@Setter
public class ServiceCategoryService {
    private final ServiceCategoryDtoMapper mapper;
    private final ServiceCategoryRepository repo;
    private final ServiceRepository serviceRepo;

    @Transactional
    public ServiceCategoryResponseDTO create(ServiceCategoryCreateDTO dto) {
        String normalized = normalize(dto.getName());
        dto.setName(normalized);

        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException("Cannot create category without name");
        }
        if (repo.existsByNameIgnoreCase(normalized)) {
            throw new DuplicateEntityException("Category '" + normalized + "' already exists");
        }

        try {
            ServiceCategory saved = mapper.create(dto);
            repo.save(saved);
            return mapper.toDto(saved);
        } catch (DataIntegrityViolationException ex) {
            if (isUniqueViolation(ex)) {
                throw new DuplicateEntityException("Category '" + normalized + "' already exists");
            }
            throw ex;
        }
    }

    @Transactional
    public ServiceCategoryResponseDTO update(ServiceCategoryUpdateDTO dto, UUID targetId) {
        String normalized = normalize(dto.getName());
        dto.setName(normalized);
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException("Cannot update category without name");
        }

        ServiceCategory target = repo.findById(targetId)
                .orElseThrow(() -> new NotFoundException("No category found with ID: " + targetId));
        String targetName = target.getName();

        if (normalized.equals(targetName)) {
            return mapper.toDto(target);
        }

        if (repo.existsByNameIgnoreCaseAndIdNot(normalized, targetId)) {
            throw new DuplicateEntityException("Category '" + normalized + "' already exists");
        }

        try {
            return mapper.toDto(repo.save(mapper.update(target, dto)));
        } catch (DataIntegrityViolationException | ConstraintViolationException ex) {
            if (isUniqueViolation(ex)) {
                throw new DuplicateEntityException("Category '" + normalized + "' already exists");
            }
            throw ex;
        }
    }

    public ServiceCategoryResponseDTO getCategory(UUID id) {
        if (id == null) throw new IllegalArgumentException("Category id is required");
        ServiceCategory category = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("No category found with ID: " + id));
        return mapper.toDto(category);
    }

    public Page<ServiceCategoryResponseDTO> getAllCategories(String search, Pageable pageable) {
        Specification<ServiceCategory> spec = ((root, q, cb) -> cb.conjunction());

        if (search != null && !search.isBlank()) {
            Specification<ServiceCategory> byName =
                    (root, q, cb) ->
                            cb.like(cb.lower(root.get("name")), "%" + search.trim().toLowerCase(Locale.ROOT) + "%");
        }

        return repo.findAll(spec, pageable)
                .map(mapper::toDto);
    }

    public List<ServiceCategoryResponseDTO> getAllCategoriesByList() {
        return repo.findAll()
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public void delete(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Category id is required");
        }
        // preflight FK usage check
        if (serviceRepo.existsByCategoryId(id)) {
            throw new ConflictException("Cannot delete category: it’s used by one or more services");
        }

        try {
            repo.deleteById(id);
        } catch (DataIntegrityViolationException | ConstraintViolationException ex) {
            throw new ConflictException("Cannot delete category: it’s referenced by other records");
        }
    }

    private String normalize(String s) {
        return s == null ? null : s.trim();
    }

    /**
     * Best-effort detection of Postgres unique constraint violations.
     * SQLState 23505 = unique_violation.
     */
    private boolean isUniqueViolation(Throwable t) {
        Throwable cause = t;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException cve) {
                String sqlState = cve.getSQLException() != null ? cve.getSQLException().getSQLState() : null;
                if (("23505").equals(sqlState)) return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
