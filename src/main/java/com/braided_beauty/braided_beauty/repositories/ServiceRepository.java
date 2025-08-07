package com.braided_beauty.braided_beauty.repositories;

import com.braided_beauty.braided_beauty.models.ServiceModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ServiceRepository extends JpaRepository<ServiceModel, UUID> {
    boolean existsByName(String name);
    Optional<ServiceModel> findTopByOrderByTimesBookedDesc();
}
