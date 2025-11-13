package com.braided_beauty.braided_beauty.repositories;

import com.braided_beauty.braided_beauty.dtos.service.ServiceResponseDTO;
import com.braided_beauty.braided_beauty.models.ServiceModel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ServiceRepository extends JpaRepository<ServiceModel, UUID>, JpaSpecificationExecutor<ServiceModel> {
    boolean existsByName(String name);
    Optional<ServiceModel> findTopByOrderByTimesBookedDesc();
    boolean existsByCategoryId(UUID id);
    @EntityGraph(attributePaths = {"category", "addOns"})
    @Nonnull
    Page<ServiceModel> findAll(@Nullable Specification<ServiceModel> spec, @Nonnull Pageable pageable);
}
